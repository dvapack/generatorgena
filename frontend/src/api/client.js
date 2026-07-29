const API_BASE_URL = (process.env.REACT_APP_API_BASE_URL || "/api").replace(
  /\/$/,
  "",
);

export class ApiError extends Error {
  constructor(message, status = 0, errors = []) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.errors = errors;
  }
}

const getAccessToken = () => localStorage.getItem("accessToken");

const expireSession = () => {
  localStorage.removeItem("accessToken");
  sessionStorage.setItem(
    "authMessage",
    "Сессия истекла. Пожалуйста, войдите снова.",
  );
  window.dispatchEvent(new Event("auth:unauthorized"));
};

const readError = async (response) => {
  try {
    const payload = await response.json();
    return new ApiError(
      payload.message || `Ошибка запроса (${response.status})`,
      response.status,
      Array.isArray(payload.errors) ? payload.errors : [],
    );
  } catch {
    return new ApiError(`Ошибка запроса (${response.status})`, response.status);
  }
};

async function request(
  path,
  { method = "GET", body, authenticated = true, responseType = "json" } = {},
) {
  const headers = {};
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  if (authenticated) {
    const token = getAccessToken();
    if (!token) {
      expireSession();
      throw new ApiError("Для продолжения необходимо войти", 401);
    }
    headers.Authorization = `Bearer ${token}`;
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError("Не удалось связаться с сервером");
  }

  if (!response.ok) {
    if (response.status === 401 && authenticated) {
      expireSession();
    }
    throw await readError(response);
  }

  if (response.status === 204) {
    return null;
  }
  return responseType === "blob" ? response.blob() : response.json();
}

export const register = (email, password) =>
  request("/users/register", {
    method: "POST",
    body: { email, password },
    authenticated: false,
  });

export const login = (email, password) =>
  request("/users/login", {
    method: "POST",
    body: { email, password },
    authenticated: false,
  });

export const createGeneration = (prompt) =>
  request("/generations", {
    method: "POST",
    body: { prompt, type: "IMAGE" },
  });

export const getGenerations = (page = 0, size = 20) =>
  request(`/generations?page=${page}&size=${size}`);

export const getGeneration = (id) => request(`/generations/${id}`);

export const getGenerationAsset = (id) =>
  request(`/generations/${id}/asset`, { responseType: "blob" });

export const updateGenerationRating = (id, rating) =>
  request(`/generations/${id}/rating`, {
    method: "PUT",
    body: { rating },
  });

export const deleteGeneration = (id) =>
  request(`/generations/${id}`, { method: "DELETE" });
