import React, { useContext, useState } from "react";
import { AuthContext } from "../context";
import style from "../styles/Light/Login.module.css";
import MyButton from "../UI/components/buttons/MyButton";
import MyInput from "../UI/components/input/MyInput";
import "bootstrap/dist/css/bootstrap.min.css";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";

const LoginLight = () => {
  // состояния для авторизации (об это я подумаю позже)
  const { isAuth, setIsAuth } = useContext(AuthContext);
  // переменные
  const [loginData, setLoginData] = useState({ name: "", password: "" });
  // не помню зачем делала, но пусть пока будут
  const [submitted, setSubmitted] = useState(false);
  // для чтения логина из поля ввода
  const handleUsernameChange = (event) => {
    const name = event.target.value;
    setLoginData({ ...loginData, name: name });
  };
  // для чтения пароля из поля ввода
  const handlePasswordChange = (event) => {
    const password = event.target.value;
    setLoginData({ ...loginData, password });
  };
  const handleRedirectToMain = () => {
    history.push("/main");
  };
  const [isRemember, setRemember] = useState();

  const login = async (event) => {
    event.preventDefault();
    setSubmitted(true);

    const url = "http://localhost:8000/api/users/login/";

    const data = {
      username: loginData.name,
      password: loginData.password,
    };
    console.log("AAAAAAAAAAAAA");
    try {
      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(data),
      });

      if (!response.ok) {
        console.error("Ошибка при выполнении запроса:");
        return;
      }

      const result = await response.json();

      localStorage.setItem("accessToken", result.access);
      localStorage.setItem("refreshToken", result.refresh);
      handleRedirectToMain();
      console.log(result);
    } catch (error) {
      console.error("Error during login:", error);
    }
  };
  // для смены языка с русского на английский и тд
  const { t, i18n } = useTranslation();
  // для перехода между страницами
  const history = useHistory();
  // при нажатии на кнопку регистрации
  const handleRedirectToRegistration = () => {
    history.push("/registration");
  };
  // при нажатии на надпись Забыли пароль?
  const handleRedirectToChangeCode = () => {
    history.push("/change-code");
  };
  const returnToLogin = () => {
    history.push("/login");
  };
  const returnOurTeam = () => {
    history.push("/ourteam");
  };
  const returnLinks = () => {
    history.push("/links");
  };
  // проверка корректны ли данные, которые введены в поля логина и пароля
  const [showForgotPassword, setShowForgotPassword] = useState(false);
  return (
    <div>
      <div className={style.MainBlur} />
      <div className={style.LoginPage}>
        <div className="container-fluid">
          <div className={style.MainPage}>
            <div className="row">
              <div className={"col-lg-2"}></div>
              <div className={`col-lg-5 text-center`}>
                <div className={style.Info}>
                  <h1 className={style.InfoText}>
                    Гена - генератор изображений
                  </h1>
                </div>
              </div>
              <div className={`col-lg-3`}>
                <div className={style.MainForm}>
                  <div className={style.MainFormBlur} />
                  <form className={style.form} onSubmit={login}>
                    <h1 className={style.MainFormHeaderText}>Войти</h1>
                    <MyInput
                      blur={true}
                      style={{ margin: "0px", marginTop: "5%" }}
                      type="text"
                      placeholder="Логин"
                      onChange={handleUsernameChange}
                    />
                    <MyInput
                      blur={true}
                      style={{ margin: "0px", marginTop: "5%" }}
                      type="password"
                      placeholder="Пароль"
                      onChange={handlePasswordChange}
                    />
                    <MyButton
                      type="submit"
                      blur={true}
                      style={{
                        margin: "0px",
                        marginTop: "8%",
                      }}
                    >
                      Вход
                    </MyButton>
                    <MyButton
                      onClick={handleRedirectToRegistration}
                      blur={true}
                      style={{
                        margin: "0px",
                        marginTop: "5%",
                        backgroundColor: "rgba(30, 30, 30, 0.1)",
                        color: "#ffffff",
                        opacity: "90%",
                      }}
                    >
                      Регистрация
                    </MyButton>
                  </form>
                </div>
              </div>
              <div className={style.Bottom}>
                <h4 className={style.BottomText}>Гена © 2025</h4>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
export default LoginLight;
