import React, { useState } from "react";
import { Link, useHistory } from "react-router-dom";
import { register } from "../api/client";
import MyButton from "../UI/components/buttons/MyButton";
import MyInput from "../UI/components/input/MyInput";
import style from "../styles/Light/Registration.module.css";

const RegLight = () => {
  const history = useHistory();
  const [form, setForm] = useState({ email: "", password: "" });
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState("");

  const handleSubmit = async (event) => {
    event.preventDefault();
    setIsLoading(true);
    setMessage("");
    try {
      await register(form.email, form.password);
      sessionStorage.setItem(
        "authMessage",
        "Регистрация завершена. Теперь войдите в аккаунт.",
      );
      history.replace("/login");
    } catch (error) {
      setMessage(
        [error.message, ...(error.errors || [])].filter(Boolean).join(". "),
      );
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <main className={style.LoginPage}>
      <div className={style.MainBlur} />
      <section className={style.Hero}>
        <p className={style.Eyebrow}>Генератор изображений</p>
        <h1 className={style.InfoText}>Познакомьтесь с Геной</h1>
        <p className={style.Lead}>
          Создайте аккаунт — ваши изображения и оценки сохранятся в истории.
        </p>
      </section>

      <section className={style.MainForm} aria-labelledby="register-heading">
        <div className={style.MainFormBlur} />
        <form className={style.form} onSubmit={handleSubmit}>
          <h2 id="register-heading" className={style.MainFormHeaderText}>
            Регистрация
          </h2>
          {message && (
            <div className={style.Error} role="alert">
              {message}
            </div>
          )}
          <MyInput
            blur
            type="email"
            autoComplete="email"
            placeholder="Email"
            aria-label="Email"
            value={form.email}
            onChange={(event) =>
              setForm((current) => ({
                ...current,
                email: event.target.value,
              }))
            }
            required
          />
          <MyInput
            blur
            type="password"
            autoComplete="new-password"
            placeholder="Пароль — от 8 символов"
            aria-label="Пароль"
            minLength={8}
            maxLength={72}
            value={form.password}
            onChange={(event) =>
              setForm((current) => ({
                ...current,
                password: event.target.value,
              }))
            }
            required
          />
          <MyButton type="submit" blur disabled={isLoading}>
            {isLoading ? "Создаём аккаунт…" : "Зарегистрироваться"}
          </MyButton>
          <p className={style.FormFooter}>
            Уже есть аккаунт? <Link to="/login">Войти</Link>
          </p>
        </form>
      </section>
      <footer className={style.Bottom}>Гена © 2026</footer>
    </main>
  );
};

export default RegLight;
