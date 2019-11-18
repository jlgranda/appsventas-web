/*
 * Copyright (C) 2019 jlgrandadev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jlgranda.fede.servlet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author jlgrandadev
 */
public class DynamicImageServlet extends HttpServlet {

    private static final long serialVersionUID = 3469834164649490600L;

    private static final int BYTES_DOWNLOAD = 1024;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        try {

            // Get image file.
            String uuid = request.getParameter("uuid");
            String type = request.getParameter("type");
            String file = "/tmp/" + uuid + "." + type;
            if ("default".equalsIgnoreCase(type)){
                file = "/var/opt/appsventas/img/emporiolojano-gift-default.png";
            }

            // Get image contents.
            byte[] bytes = new byte[BYTES_DOWNLOAD];

            int read = 0;

            File f = new File(file);

            int evitarCicloInfinito = 0;
            //Esperar a que el archivo de imagen exista en el disco duro o realice 3 intentos
            while (!f.exists() && evitarCicloInfinito < 3) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException ex) {
                    Logger.getLogger(DynamicImageServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
                evitarCicloInfinito++;
            };

            if (f.exists() && !f.isDirectory()) {
                // All response status is defined in the HttpServletResponse class. We
                // can then use these constants value to return process status to the
                // browser.
                // Let say this servlet only handle request for page name inputForm. So
                // when user request for other page name error page not found 404 will
                // be returned, other wise it will be 200 which mean OK.
                response.setContentType("image/png");
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
                OutputStream os = response.getOutputStream();
                while ((read = in.read(bytes)) != -1) {
                    os.write(bytes, 0, read);
//                    imagenLeida = true;
                }
                os.flush();
//                } 
            } else {
//                response.setContentType("text/html");
//                response.sendError(HttpServletResponse.SC_NOT_FOUND, "The requested image ["
//                        + uuid + "] not found.");
                String page = request.getContextPath() + "/images/dynamic/?uuid=appsventas&type=default";
                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(page);
                dispatcher.forward(request, response);
            }

        } catch (IOException e) {
            response.setContentType("text/html");
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "The requested image not found. " + e.getMessage());
        }

    }

}
