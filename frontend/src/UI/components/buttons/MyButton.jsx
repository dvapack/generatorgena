// MyButton.jsx
import React from "react";
import style from "../../../styles/MyButton.module.css";

const MyButton = ({ children, blur, ...props }) => {
  return (
    <button
      {...props}
      className={`${style.MyButton} ${blur ? style.MyButtonBlur : ""}`}
    >
      {children}
    </button>
  );
};

export default MyButton;
