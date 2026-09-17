import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import {
  createGeneration,
  deleteGeneration,
  getGeneration,
  getGenerationAsset,
  getGenerations,
  updateGenerationRating,
} from "../api/client";
import { AuthContext } from "../context";
import MyButton from "../UI/components/buttons/MyButton";
import style from "../styles/Light/Main.module.css";

const PAGE_SIZE = 20;

const STATUS_LABELS = {
  QUEUED: "В очереди",
  PROCESSING: "Генерируется",
  COMPLETED: "Готово",
  FAILED: "Не удалось",
};

const formatDate = (value) => {
  if (!value) return "";
  return new Intl.DateTimeFormat("ru-RU", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
};

const errorText = (error) =>
  [error.message, ...(error.errors || [])].filter(Boolean).join(". ");

const MainLight = () => {
  const { signOut } = useContext(AuthContext);
  const [prompt, setPrompt] = useState("");
  const [isCreating, setIsCreating] = useState(false);
  const [selected, setSelected] = useState(null);
  const [assetUrl, setAssetUrl] = useState("");
  const [generations, setGenerations] = useState([]);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [isHistoryLoading, setIsHistoryLoading] = useState(true);
  const [feedback, setFeedback] = useState("");
  const [ratingLoading, setRatingLoading] = useState(false);
  const [deletingId, setDeletingId] = useState(null);

  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const loadHistory = useCallback(async (requestedPage = 0) => {
    setIsHistoryLoading(true);
    try {
      const result = await getGenerations(requestedPage, PAGE_SIZE);
      setGenerations(result.generations);
      setPage(result.page);
      setTotal(result.total);
    } catch (error) {
      setFeedback(errorText(error));
    } finally {
      setIsHistoryLoading(false);
    }
  }, []);

  useEffect(() => {
    loadHistory(0);
  }, [loadHistory]);

  useEffect(() => {
    if (
      !selected?.id ||
      !["QUEUED", "PROCESSING"].includes(selected.status)
    ) {
      return undefined;
    }

    let cancelled = false;
    const poll = async () => {
      try {
        const generationEntity = await getGeneration(selected.id);
        if (!cancelled) {
          setSelected(generationEntity);
          setGenerations((current) =>
            current.map((item) =>
              item.id === generationEntity.id ? generationEntity : item,
            ),
          );
          if (["COMPLETED", "FAILED"].includes(generationEntity.status)) {
            loadHistory(0);
          }
        }
      } catch (error) {
        if (!cancelled) setFeedback(errorText(error));
      }
    };

    poll();
    const intervalId = window.setInterval(poll, 2000);
    return () => {
      cancelled = true;
      window.clearInterval(intervalId);
    };
  }, [selected?.id, selected?.status, loadHistory]);

  useEffect(() => {
    if (!selected?.id || selected.status !== "COMPLETED") {
      setAssetUrl("");
      return undefined;
    }

    let cancelled = false;
    let objectUrl = "";
    const loadAsset = async () => {
      try {
        const blob = await getGenerationAsset(selected.id);
        if (!cancelled) {
          objectUrl = URL.createObjectURL(blob);
          setAssetUrl(objectUrl);
        }
      } catch (error) {
        if (!cancelled) setFeedback(errorText(error));
      }
    };
    setAssetUrl("");
    loadAsset();

    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [selected?.id, selected?.status]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    const normalizedPrompt = prompt.trim();
    if (!normalizedPrompt) {
      setFeedback("Введите описание изображения");
      return;
    }

    setIsCreating(true);
    setFeedback("");
    try {
      const created = await createGeneration(normalizedPrompt);
      setSelected({
        ...created,
        prompt: normalizedPrompt,
        rating: null,
        asset: null,
      });
      setPrompt("");
      setPage(0);
      await loadHistory(0);
    } catch (error) {
      setFeedback(errorText(error));
    } finally {
      setIsCreating(false);
    }
  };

  const handleSelect = async (generationEntity) => {
    setFeedback("");
    try {
      setSelected(await getGeneration(generationEntity.id));
    } catch (error) {
      setFeedback(errorText(error));
    }
  };

  const handleRating = async (rating) => {
    if (!selected || ratingLoading) return;
    setRatingLoading(true);
    setFeedback("");
    try {
      await updateGenerationRating(selected.id, rating);
      setSelected((current) => ({ ...current, rating }));
      setGenerations((current) =>
        current.map((item) =>
          item.id === selected.id ? { ...item, rating } : item,
        ),
      );
    } catch (error) {
      setFeedback(errorText(error));
    } finally {
      setRatingLoading(false);
    }
  };

  const handleDelete = async (generationEntity) => {
    if (
      !window.confirm(
        `Удалить генерацию «${generationEntity.prompt || "Без названия"}»?`,
      )
    ) {
      return;
    }
    setDeletingId(generationEntity.id);
    setFeedback("");
    try {
      await deleteGeneration(generationEntity.id);
      if (selected?.id === generationEntity.id) setSelected(null);
      const nextPage =
        generations.length === 1 && page > 0 ? page - 1 : page;
      await loadHistory(nextPage);
    } catch (error) {
      setFeedback(errorText(error));
    } finally {
      setDeletingId(null);
    }
  };

  const selectedStatus = selected
    ? STATUS_LABELS[selected.status] || selected.status
    : "";

  const pageLabel = useMemo(
    () => `${Math.min(page + 1, totalPages)} / ${totalPages}`,
    [page, totalPages],
  );

  return (
    <main className={style.Page}>
      <header className={style.Header}>
        <div>
          <span className={style.Logo}>Гена</span>
          <span className={style.Tagline}>генератор изображений</span>
        </div>
        <button className={style.LogoutButton} type="button" onClick={signOut}>
          Выйти
        </button>
      </header>

      {feedback && (
        <div className={style.Feedback} role="alert">
          <span>{feedback}</span>
          <button
            type="button"
            aria-label="Закрыть сообщение"
            onClick={() => setFeedback("")}
          >
            ×
          </button>
        </div>
      )}

      <div className={style.Workspace}>
        <section className={style.ResultPanel} aria-live="polite">
          {!selected && (
            <div className={style.EmptyResult}>
              <span className={style.Spark}>✦</span>
              <h1>Вообразите — Гена нарисует</h1>
              <p>
                Опишите сюжет, настроение и детали будущего изображения.
              </p>
            </div>
          )}

          {selected && selected.status !== "COMPLETED" && (
            <div className={style.EmptyResult}>
              <span
                className={`${style.StatusOrb} ${
                  selected.status === "FAILED" ? style.FailedOrb : ""
                }`}
              />
              <p className={style.StatusLabel}>{selectedStatus}</p>
              <h2>{selected.prompt}</h2>
              {selected.status === "FAILED" && (
                <p>Попробуйте изменить описание и запустить генерацию снова.</p>
              )}
            </div>
          )}

          {selected?.status === "COMPLETED" && (
            <div className={style.CompletedResult}>
              {assetUrl ? (
                <img src={assetUrl} alt={selected.prompt} />
              ) : (
                <div className={style.ImageLoading}>Загружаем изображение…</div>
              )}
              <div className={style.ResultMeta}>
                <div>
                  <span className={style.StatusBadge}>Готово</span>
                  <p>{selected.prompt}</p>
                </div>
                <div
                  className={style.Rating}
                  aria-label="Оценка изображения"
                >
                  {[1, 2, 3, 4, 5].map((rating) => (
                    <button
                      type="button"
                      key={rating}
                      className={
                        rating <= (selected.rating || 0)
                          ? style.StarActive
                          : ""
                      }
                      aria-label={`Оценить на ${rating}`}
                      disabled={ratingLoading}
                      onClick={() => handleRating(rating)}
                    >
                      ★
                    </button>
                  ))}
                </div>
              </div>
            </div>
          )}
        </section>

        <aside className={style.Controls}>
          <form className={style.PromptForm} onSubmit={handleSubmit}>
            <label htmlFor="generationEntity-prompt">Что будем создавать?</label>
            <textarea
              id="generationEntity-prompt"
              value={prompt}
              maxLength={2000}
              onChange={(event) => setPrompt(event.target.value)}
              placeholder="Например: уютный домик в волшебном лесу на закате…"
            />
            <div className={style.PromptFooter}>
              <span>{prompt.length} / 2000</span>
              <MyButton
                type="submit"
                blur
                disabled={isCreating || !prompt.trim()}
              >
                {isCreating ? "Отправляем…" : "Сгенерировать"}
              </MyButton>
            </div>
          </form>

          <section className={style.History} aria-labelledby="history-heading">
            <div className={style.HistoryHeader}>
              <div>
                <p className={style.Eyebrow}>Ваши работы</p>
                <h2 id="history-heading">История</h2>
              </div>
              <button type="button" onClick={() => loadHistory(page)}>
                Обновить
              </button>
            </div>

            {isHistoryLoading ? (
              <p className={style.HistoryMessage}>Загружаем историю…</p>
            ) : generations.length === 0 ? (
              <p className={style.HistoryMessage}>
                Здесь появятся созданные изображения.
              </p>
            ) : (
              <ul className={style.HistoryList}>
                {generations.map((generationEntity) => (
                  <li
                    key={generationEntity.id}
                    className={
                      selected?.id === generationEntity.id ? style.SelectedItem : ""
                    }
                  >
                    <button
                      type="button"
                      className={style.HistoryItem}
                      onClick={() => handleSelect(generationEntity)}
                    >
                      <span
                        className={`${style.HistoryStatus} ${
                          style[`Status${generationEntity.status}`] || ""
                        }`}
                      />
                      <span className={style.HistoryText}>
                        <strong>{generationEntity.prompt}</strong>
                        <small>
                          {STATUS_LABELS[generationEntity.status]} ·{" "}
                          {formatDate(generationEntity.createdAt)}
                        </small>
                      </span>
                      {generationEntity.rating && (
                        <span className={style.HistoryRating}>
                          ★ {generationEntity.rating}
                        </span>
                      )}
                    </button>
                    <button
                      type="button"
                      className={style.DeleteButton}
                      aria-label={`Удалить генерацию ${generationEntity.prompt}`}
                      disabled={deletingId === generationEntity.id}
                      onClick={() => handleDelete(generationEntity)}
                    >
                      {deletingId === generationEntity.id ? "…" : "×"}
                    </button>
                  </li>
                ))}
              </ul>
            )}

            {total > PAGE_SIZE && (
              <nav className={style.Pagination} aria-label="Страницы истории">
                <button
                  type="button"
                  disabled={page === 0 || isHistoryLoading}
                  onClick={() => loadHistory(page - 1)}
                >
                  Назад
                </button>
                <span>{pageLabel}</span>
                <button
                  type="button"
                  disabled={page + 1 >= totalPages || isHistoryLoading}
                  onClick={() => loadHistory(page + 1)}
                >
                  Вперёд
                </button>
              </nav>
            )}
          </section>
        </aside>
      </div>
    </main>
  );
};

export default MainLight;
