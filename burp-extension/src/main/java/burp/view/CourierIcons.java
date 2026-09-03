/*
 * Copyright Praetorian Security Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package burp.view;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;

public final class CourierIcons {
    private static final Icon MERCURY_HELMET = loadMercuryHelmet();
    private CourierIcons() {
    }

    public static Icon mercuryHelmet() {
        return MERCURY_HELMET;
    }

    public static Icon mercuryHelmet(int size) {
        if (!(MERCURY_HELMET instanceof ImageIcon imageIcon) || size <= 0) {
            return MERCURY_HELMET;
        }
        Image scaled = imageIcon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    public static Icon control() {
        return new VectorIcon(Kind.CONTROL);
    }

    public static Icon planner() {
        return new VectorIcon(Kind.PLANNER);
    }

    public static Icon webflows() {
        return new VectorIcon(Kind.WEBFLOW);
    }

    public static Icon copy() {
        return new VectorIcon(Kind.COPY);
    }

    private enum Kind { CONTROL, PLANNER, WEBFLOW, COPY }

    private static Icon loadMercuryHelmet() {
        try {
            BufferedImage image = ImageIO.read(
                    CourierIcons.class.getResource("/icons/mercury.png"));
            return new ImageIcon(image);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Unable to load Mercury icon", exception);
        }
    }

    private record VectorIcon(Kind kind) implements Icon {
        @Override public int getIconWidth() { return 15; }
        @Override public int getIconHeight() { return 15; }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.translate(x, y);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(CourierTheme.ACCENT);
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                switch (kind) {
                    case CONTROL -> paintControl(g);
                    case PLANNER -> paintPlanner(g);
                    case WEBFLOW -> paintWebflow(g);
                    case COPY -> paintCopy(g);
                }
            } finally {
                g.dispose();
            }
        }

        private static void paintControl(Graphics2D g) {
            g.drawOval(2, 2, 10, 10);
            g.fillOval(6, 6, 3, 3);
            g.drawLine(7, 0, 7, 4);
        }

        private static void paintPlanner(Graphics2D g) {
            g.drawLine(7, 0, 7, 14);
            g.drawLine(0, 7, 14, 7);
            g.drawLine(2, 2, 12, 12);
            g.drawLine(12, 2, 2, 12);
            g.fillOval(5, 5, 5, 5);
        }

        private static void paintCopy(Graphics2D g) {
            g.drawRoundRect(2, 4, 8, 8, 2, 2);
            g.drawRoundRect(5, 1, 8, 8, 2, 2);
        }

        private static void paintWebflow(Graphics2D g) {
            g.drawLine(3, 3, 11, 7);
            g.drawLine(11, 7, 3, 12);
            g.fillOval(0, 0, 6, 6);
            g.fillOval(8, 4, 6, 6);
            g.fillOval(0, 9, 6, 6);
        }
    }
}
