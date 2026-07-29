import {
  ApiError,
  createGeneration,
  deleteGeneration,
  getGenerationAsset,
  login,
} from "./client";

const jsonResponse = (payload, status = 200) => ({
  ok: status >= 200 && status < 300,
  status,
  json: jest.fn().mockResolvedValue(payload),
});

describe("API client", () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    global.fetch = jest.fn();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test("logs in with the new email/password contract", async () => {
    fetch.mockResolvedValue(
      jsonResponse({
        accessToken: "jwt-token",
        tokenType: "Bearer",
        expiresIn: 900,
      }),
    );

    await expect(login("gena@example.com", "password")).resolves.toEqual(
      expect.objectContaining({ accessToken: "jwt-token" }),
    );
    expect(fetch).toHaveBeenCalledWith("/api/users/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: "gena@example.com",
        password: "password",
      }),
    });
  });

  test("creates an IMAGE generation with a bearer token", async () => {
    localStorage.setItem("accessToken", "jwt-token");
    fetch.mockResolvedValue(
      jsonResponse({ id: "generation-id", type: "IMAGE", status: "QUEUED" }, 201),
    );

    await createGeneration("Розовый космический корабль");

    expect(fetch).toHaveBeenCalledWith("/api/generations", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer jwt-token",
      },
      body: JSON.stringify({
        prompt: "Розовый космический корабль",
        type: "IMAGE",
      }),
    });
  });

  test("returns protected assets as blobs", async () => {
    const image = new Blob(["image"], { type: "image/png" });
    localStorage.setItem("accessToken", "jwt-token");
    fetch.mockResolvedValue({
      ok: true,
      status: 200,
      blob: jest.fn().mockResolvedValue(image),
    });

    await expect(getGenerationAsset("generation-id")).resolves.toBe(image);
    expect(fetch).toHaveBeenCalledWith(
      "/api/generations/generation-id/asset",
      expect.objectContaining({
        headers: { Authorization: "Bearer jwt-token" },
      }),
    );
  });

  test("supports empty 204 responses", async () => {
    localStorage.setItem("accessToken", "jwt-token");
    fetch.mockResolvedValue({ ok: true, status: 204 });

    await expect(deleteGeneration("generation-id")).resolves.toBeNull();
  });

  test("expires the local session after an authenticated 401", async () => {
    localStorage.setItem("accessToken", "expired-token");
    const unauthorized = jest.fn();
    window.addEventListener("auth:unauthorized", unauthorized);
    fetch.mockResolvedValue(
      jsonResponse(
        {
          message: "Для продолжения необходимо авторизоваться",
          errors: [],
        },
        401,
      ),
    );

    await expect(createGeneration("Тест")).rejects.toEqual(
      expect.objectContaining({
        name: "ApiError",
        status: 401,
      }),
    );
    expect(localStorage.getItem("accessToken")).toBeNull();
    expect(sessionStorage.getItem("authMessage")).toMatch(/Сессия истекла/);
    expect(unauthorized).toHaveBeenCalledTimes(1);
    window.removeEventListener("auth:unauthorized", unauthorized);
  });

  test("preserves backend validation details", async () => {
    fetch.mockResolvedValue(
      jsonResponse(
        {
          message: "Ошибка валидации",
          errors: ["password: Пароль должен содержать от 8 до 72 символов"],
        },
        400,
      ),
    );

    await expect(login("gena@example.com", "short")).rejects.toEqual(
      new ApiError(
        "Ошибка валидации",
        400,
        ["password: Пароль должен содержать от 8 до 72 символов"],
      ),
    );
  });
});
