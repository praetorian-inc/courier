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

import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.Color;
import java.awt.Graphics;

final class CourierSplitPaneUI extends BasicSplitPaneUI {
    @Override
    public BasicSplitPaneDivider createDefaultDivider() {
        return new Divider(this);
    }

    private static final class Divider extends BasicSplitPaneDivider {
        private Divider(BasicSplitPaneUI ui) {
            super(ui);
            setBorder(new EmptyBorder(0, 0, 0, 0));
            setBackground(CourierTheme.background());
        }

        @Override
        public void paint(Graphics graphics) {
            graphics.setColor(CourierTheme.background());
            graphics.fillRect(0, 0, getWidth(), getHeight());
            JSplitPane splitPane = getBasicSplitPaneUI().getSplitPane();
            boolean horizontal = splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT;
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            graphics.setColor(CourierTheme.elevatedSurface());
            if (horizontal) {
                graphics.fillRoundRect(centerX - 3, centerY - 13, 7, 27, 7, 7);
            } else {
                graphics.fillRoundRect(centerX - 13, centerY - 3, 27, 7, 7, 7);
            }
            graphics.setColor(CourierTheme.muted());
            for (int offset = -6; offset <= 6; offset += 6) {
                int x = horizontal ? centerX : centerX + offset;
                int y = horizontal ? centerY + offset : centerY;
                graphics.fillOval(x - 2, y - 2, 4, 4);
            }
        }
    }
}
