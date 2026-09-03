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

import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public final class CourierToggle extends JToggleButton {
    public CourierToggle(boolean selected) {
        setSelected(selected);
        setPreferredSize(new Dimension(42, 23));
        setMinimumSize(new Dimension(42, 23));
        setMaximumSize(new Dimension(42, 23));
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(selected ? "Enabled" : "Disabled");
        addItemListener(event -> {
            setToolTipText(isSelected() ? "Enabled" : "Disabled");
            repaint();
        });
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color track = isSelected() ? CourierTheme.ACCENT : CourierTheme.borderColor();
            g.setColor(track);
            g.fillRoundRect(1, 3, 39, 17, 17, 17);
            g.setColor(isSelected() ? Color.WHITE : CourierTheme.muted());
            int knobX = isSelected() ? 23 : 4;
            g.fillOval(knobX, 5, 13, 13);
            if (isFocusOwner()) {
                g.setColor(CourierTheme.ACCENT_HOVER);
                g.drawRoundRect(0, 2, 40, 19, 19, 19);
            }
        } finally {
            g.dispose();
        }
    }
}
