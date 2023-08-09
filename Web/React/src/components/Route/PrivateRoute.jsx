import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useSelector } from "react-redux";

const PrivateRoute = () => {
    console.log("private route 작동")
    const isAuthenticated = useSelector((state) => {
        return state.auth.isAuthenticated;
    });

    if(isAuthenticated) {
        // 로그인이 된 상태라면 해당 컴포넌트 렌더링
        return <Outlet />;
    } else {
        alert("로그인 후 이용가능합니다.");
        // 로그인 안된 상태라면 메인 페이지로 이동
        return <Navigate replace to="/" />;
    }
    
};

export default PrivateRoute;