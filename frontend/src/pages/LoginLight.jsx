import React, { useContext, useEffect, useState } from "react";
import { Link, useHistory } from "react-router-dom";
import { login } from "../api/client";
import { AuthContext } from "../context";
import MyButton from "../UI/components/buttons/MyButton";
import MyInput from "../UI/components/input/MyInput";
import style from "../styles/Light/Login.module.css";

const LoginLight = () => {
  const history = useHistory();
  const { signIn } = useContext(AuthContext);
  const [form, setForm] = useState({ email: "", password: "" });
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    const authMessage = sessionStorage.getItem("authMessage");
    if (authMessage) {
      setMessage(authMessage);
      sessionStorage.removeItem("authMessage");
    }
  }, []);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setIsLoading(true);
    setMessage("");
    try {
      const result = await login(form.email, form.password);
      signIn(result.accessToken);
      history.replace("/main");
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
        <h1 className={style.InfoText}>Создавайте необычное вместе с Геной</h1>
        <p className={style.Lead}>
          Войдите, чтобы запускать генерации и возвращаться к своим работам.
        </p>
      </section>

      <section className={style.MainForm} aria-labelledby="login-heading">
        <div className={style.MainFormBlur} />
        <form className={style.form} onSubmit={handleSubmit}>
          <h2 id="login-heading" className={style.MainFormHeaderText}>
            Вход
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
            autoComplete="current-password"
            placeholder="Пароль"
            aria-label="Пароль"
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
            {isLoading ? "Входим…" : "Войти"}
          </MyButton>
          <p className={style.FormFooter}>
            Нет аккаунта? <Link to="/registration">Зарегистрироваться</Link>
          </p>
        </form>
      </section>
      <footer className={style.Bottom}>Гена © 2026</footer>
    </main>
  );
};

export default LoginLight;
