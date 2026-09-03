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

import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.Graphics;
import java.awt.Insets;

final class CourierTabbedPaneUI extends BasicTabbedPaneUI {
    @Override
    protected void installDefaults() {
        super.installDefaults();
        tabInsets = new Insets(9, 15, 9, 15);
        selectedTabPadInsets = new Insets(0, 0, 0, 0);
        contentBorderInsets = new Insets(1, 0, 0, 0);
        tabAreaInsets = new Insets(0, 0, 0, 0);
    }

    @Override
    protected void paintTabBackground(Graphics graphics, int placement, int index,
            int x, int y, int width, int height, boolean selected) {
        graphics.setColor(selected ? CourierTheme.elevatedSurface() : CourierTheme.background());
        graphics.fillRect(x, y, width, height);
    }

    @Override
    protected void paintTabBorder(Graphics graphics, int placement, int index,
            int x, int y, int width, int height, boolean selected) {
        if (selected) {
            graphics.setColor(CourierTheme.ACCENT);
            graphics.fillRect(x, y + height - 3, width, 3);
        }
    }

    @Override
    protected void paintFocusIndicator(Graphics graphics, int placement, java.awt.Rectangle[] rectangles,
            int index, java.awt.Rectangle iconRectangle, java.awt.Rectangle textRectangle,
            boolean selected) {
        // Burp's containing suite tab already communicates focus.
    }

    @Override
    protected void paintContentBorder(Graphics graphics, int placement, int selectedIndex) {
        graphics.setColor(CourierTheme.borderColor());
        graphics.drawLine(0, 0, tabPane.getWidth(), 0);
    }
}
