package com.swp391.g1.controller;

import java.io.IOException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.swp391.g1.service.AccessPolicy;
import com.swp391.g1.util.AuthContext;

@WebServlet("/home")
public class HomeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // UI hint only: AccountServlet still enforces the ADMIN rule server-side.
        request.setAttribute("canManageAccounts",
                AccessPolicy.canManageAccounts(AuthContext.getCurrentUser(request)));
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/views/home/home.jsp");
        dispatcher.forward(request, response);
    }
}