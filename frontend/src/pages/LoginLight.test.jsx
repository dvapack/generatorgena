import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { Router } from "react-router-dom";
import { createMemoryHistory } from "history";
import { login } from "../api/client";
import { AuthContext } from "../context";
import LoginLight from "./LoginLight";

jest.mock("../api/client", () => ({
  login: jest.fn(),
}));

test("stores a successful login through AuthContext and opens main", async () => {
  const history = createMemoryHistory({ initialEntries: ["/login"] });
  const signIn = jest.fn();
  login.mockResolvedValue({
    accessToken: "new-token",
    tokenType: "Bearer",
    expiresIn: 900,
  });

  render(
    <AuthContext.Provider value={{ isAuth: false, signIn, signOut: jest.fn() }}>
      <Router history={history}>
        <LoginLight />
      </Router>
    </AuthContext.Provider>,
  );

  fireEvent.change(screen.getByLabelText("Email"), {
    target: { value: "gena@example.com" },
  });
  fireEvent.change(screen.getByLabelText("Пароль"), {
    target: { value: "strong-password" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Войти" }));

  await waitFor(() => {
    expect(login).toHaveBeenCalledWith(
      "gena@example.com",
      "strong-password",
    );
    expect(signIn).toHaveBeenCalledWith("new-token");
    expect(history.location.pathname).toBe("/main");
  });
});
