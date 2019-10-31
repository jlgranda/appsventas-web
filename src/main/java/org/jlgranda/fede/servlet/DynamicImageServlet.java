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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException

    {

        try {

            // Get image file.
            String uuid = request.getParameter("uuid");
            String type = request.getParameter("type");
            String file = "/tmp/" + uuid + "." + type;

            BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));

            // Get image contents.
            byte[] bytes = new byte[BYTES_DOWNLOAD];
            
            response.setContentType("image/png");
            
            int read = 0;
            OutputStream os = response.getOutputStream();
            while ((read = in.read(bytes)) != -1) {
                os.write(bytes, 0, read);
            }
            os.flush();

        } catch (IOException e) {
        }

    }

}
