
import Login from "../pages/Login.jsx";
import Main from "../pages/Main.jsx";
import Registration from "../pages/Registration.jsx";

export const privateRoutes = [
  { path: "/main", component: Main, exact: true },
];

export const publicRoutes = [
  { path: "/login", component: Login, exact: true },
  { path: "/registration", component: Registration, exact: true },
];
