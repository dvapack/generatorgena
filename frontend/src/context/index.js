import { createContext } from "react";

export const AuthContext = createContext({
  isAuth: false,
  accessToken: null,
  signIn: () => {},
  signOut: () => {},
});
