
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { AuthContext } from "./context";
import AppRouter from "./components/AppRouter";

function App() {
  const [accessToken, setAccessToken] = useState(() =>
    localStorage.getItem("accessToken"),
  );

  const signIn = useCallback((token) => {
    localStorage.setItem("accessToken", token);
    setAccessToken(token);
  }, []);

  const signOut = useCallback(() => {
    localStorage.removeItem("accessToken");
    setAccessToken(null);
  }, []);

  useEffect(() => {
    const handleUnauthorized = () => signOut();
    window.addEventListener("auth:unauthorized", handleUnauthorized);
    return () =>
      window.removeEventListener("auth:unauthorized", handleUnauthorized);
  }, [signOut]);

  const auth = useMemo(
    () => ({
      accessToken,
      isAuth: Boolean(accessToken),
      signIn,
      signOut,
    }),
    [accessToken, signIn, signOut],
  );

  return (
    <AuthContext.Provider value={auth}>
      <AppRouter />
    </AuthContext.Provider>
  );
}

export default App;
