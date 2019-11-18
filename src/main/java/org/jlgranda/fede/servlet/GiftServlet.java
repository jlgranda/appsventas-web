/*
 * Copyright 2013 jlgranda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jlgranda.fede.servlet;

import java.io.IOException;
import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jlgranda.fede.controller.SettingHome;

/**
 *
 * @author Ronny Ramirez
 */
@WebServlet(name = "giftServlet", urlPatterns = {"/giftServlet/*"})
public class GiftServlet extends HttpServlet {

    private static final long serialVersionUID = -2987738476456412232L;
    
    @Inject
    private SettingHome settingHome;

// Initializes the servlet.
    @Override
    public void init(ServletConfig config) throws
            ServletException {
        super.init(config);
    }

    // Destroys the servlet.
    @Override
    public void destroy() {
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, java.io.IOException {
        String page = null;

        try {

            //codigo appsventas
            String code = request.getParameter("code");
            String name = request.getParameter("name");
            String uuid = request.getParameter("uuid");
            String baseImage = request.getParameter("baseImage");

            String ar0 = settingHome.getValue("app.gift.baseImagePath", "/var/opt/appsventas/img/") + baseImage; //TODO injectar controlador 
            String ar1 = "/tmp/" + uuid + ".png";
            String ar2 = "http://emporiolojano.com:8080/appsventas-web/gift.jsf?giftuuid=" + uuid;
            String ar3 = code + " - " + name; // + "," + mobileNumber + "," + email; 2019-11-12 sólo se imprime C.I y nombres
            ar3 = ar3.replace(" ", "%20");

            String command = "java -jar /var/opt/workspace/appsventas-gifts-single-jar.jar "
                    + ar0 + " " + ar1 + " " + ar2 + " " + ar3;
            
            Runtime.getRuntime().exec(command);

            page = request.getContextPath() + "/pages/home.jsf?giftuuid=" + uuid + "&faces-redirect=true";

        } catch (IOException e) {
            page = "/404.jsf";
        }
        // dispatch control to view
        dispatch(request, response, page);
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet
     * @throws javax.servlet.ServletException request
     * @param response servlet response
     */
    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, java.io.IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, java.io.IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet
     */
    @Override
    public String getServletInfo() {
        return "Front Controller Pattern"
                + " Servlet Front Strategy";
    }

    protected void dispatch(HttpServletRequest request,
            HttpServletResponse response,
            String page)
            throws javax.servlet.ServletException,
            java.io.IOException {
//        RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(page);
//        dispatcher.forward(request, response);
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0); // Proxies.
        response.sendRedirect(page);  
    }

}
