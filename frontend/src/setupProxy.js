const { createProxyMiddleware } = require("http-proxy-middleware");

module.exports = function setupProxy(app) {
  app.use(
    "/api",
    createProxyMiddleware({
      target: process.env.REACT_APP_DEV_API_TARGET || "http://localhost:8080",
      changeOrigin: true,
      pathRewrite: { "^/api": "" },
    }),
  );
};
