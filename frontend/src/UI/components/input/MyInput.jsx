import React from "react";
import style from "../../../styles/MyInput.module.css";

const MyInput = React.forwardRef(({ blur, ...props }, ref) => {
  return (
    <input
      ref={ref}
      {...props}
      className={`${style.MyInput} ${blur ? style.MyInputBlur : ""}`}
    />
  );
});

export default MyInput;
