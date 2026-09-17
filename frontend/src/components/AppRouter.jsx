
import React, { useContext } from "react";
import { Redirect, Route, Switch } from "react-router-dom";
import { privateRoutes, publicRoutes } from "../router";
import { AuthContext } from "../context";
import { BrowserRouter as Router } from "react-router-dom";

const AppRouter = () => {
  const { isAuth } = useContext(AuthContext);

  return (
    <Router>
      <Switch>
        {privateRoutes.map((route) => (
          <Route
            path={route.path}
            exact={route.exact}
            key={route.path}
            render={(props) =>
              isAuth ? (
                <route.component {...props} />
              ) : (
                <Redirect to="/login" />
              )
            }
          />
        ))}
        {publicRoutes.map((route) => (
          <Route
            path={route.path}
            exact={route.exact}
            key={route.path}
            render={(props) =>
              isAuth ? (
                <Redirect to="/main" />
              ) : (
                <route.component {...props} />
              )
            }
          />
        ))}
        <Redirect to={isAuth ? "/main" : "/login"} />
      </Switch>
    </Router>
  );
};

export default AppRouter;
