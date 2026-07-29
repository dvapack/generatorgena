import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import {
  createGeneration,
  deleteGeneration,
  getGeneration,
  getGenerationAsset,
  getGenerations,
  updateGenerationRating,
} from "../api/client";
import { AuthContext } from "../context";
import MainLight from "./MainLight";

jest.mock("../api/client", () => ({
  createGeneration: jest.fn(),
  deleteGeneration: jest.fn(),
  getGeneration: jest.fn(),
  getGenerationAsset: jest.fn(),
  getGenerations: jest.fn(),
  updateGenerationRating: jest.fn(),
}));

beforeEach(() => {
  jest.clearAllMocks();
  URL.createObjectURL = jest.fn(() => "blob:generated-image");
  URL.revokeObjectURL = jest.fn();
  getGenerations.mockResolvedValue({
    generations: [],
    page: 0,
    size: 20,
    total: 0,
  });
});

afterEach(() => {
  jest.restoreAllMocks();
});

test("loads an empty history and submits an IMAGE generation", async () => {
  createGeneration.mockResolvedValue({
    id: "generation-id",
    type: "IMAGE",
    status: "QUEUED",
  });
  getGeneration.mockReturnValue(new Promise(() => {}));

  render(
    <AuthContext.Provider
      value={{ isAuth: true, signIn: jest.fn(), signOut: jest.fn() }}
    >
      <MainLight />
    </AuthContext.Provider>,
  );

  expect(
    await screen.findByText("Здесь появятся созданные изображения."),
  ).toBeInTheDocument();

  fireEvent.change(screen.getByLabelText("Что будем создавать?"), {
    target: { value: "  Кот-космонавт  " },
  });
  fireEvent.click(screen.getByRole("button", { name: "Сгенерировать" }));

  await waitFor(() => {
    expect(createGeneration).toHaveBeenCalledWith("Кот-космонавт");
    expect(screen.getByText("В очереди")).toBeInTheDocument();
  });
});

test("opens, rates and deletes a completed generation", async () => {
  const completed = {
    id: "completed-id",
    type: "IMAGE",
    prompt: "Город будущего",
    status: "COMPLETED",
    rating: null,
    createdAt: "2026-07-29T12:00:00+04:00",
    completedAt: "2026-07-29T12:01:00+04:00",
    asset: {
      id: "asset-id",
      assetType: "IMAGE",
      contentType: "image/png",
    },
  };
  getGenerations
    .mockResolvedValueOnce({
      generations: [completed],
      page: 0,
      size: 20,
      total: 1,
    })
    .mockResolvedValue({
      generations: [],
      page: 0,
      size: 20,
      total: 0,
    });
  getGeneration.mockResolvedValue(completed);
  getGenerationAsset.mockResolvedValue(
    new Blob(["image"], { type: "image/png" }),
  );
  updateGenerationRating.mockResolvedValue(null);
  deleteGeneration.mockResolvedValue(null);
  jest.spyOn(window, "confirm").mockReturnValue(true);

  render(
    <AuthContext.Provider
      value={{ isAuth: true, signIn: jest.fn(), signOut: jest.fn() }}
    >
      <MainLight />
    </AuthContext.Provider>,
  );

  const historyTitle = await screen.findByText("Город будущего");
  fireEvent.click(historyTitle.closest("button"));

  const fourthStar = await screen.findByRole("button", {
    name: "Оценить на 4",
  });
  fireEvent.click(fourthStar);
  await waitFor(() => {
    expect(updateGenerationRating).toHaveBeenCalledWith("completed-id", 4);
    expect(getGenerationAsset).toHaveBeenCalledWith("completed-id");
  });

  fireEvent.click(
    screen.getByRole("button", {
      name: "Удалить генерацию Город будущего",
    }),
  );
  await waitFor(() => {
    expect(deleteGeneration).toHaveBeenCalledWith("completed-id");
    expect(screen.getByText("Вообразите — Гена нарисует")).toBeInTheDocument();
  });
});
