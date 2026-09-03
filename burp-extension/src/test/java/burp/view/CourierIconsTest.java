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

import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourierIconsTest {
    @Test
    void rendersProvidedMercuryArtworkAsHeaderIcon() {
        Icon icon = CourierIcons.mercuryHelmet();
        BufferedImage image = new BufferedImage(
                icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            icon.paintIcon(null, graphics, 0, 0);
        } finally {
            graphics.dispose();
        }

        assertEquals(42, icon.getIconWidth());
        assertEquals(42, icon.getIconHeight());
        assertTrue(hasPaint(image, 0, 11));
        assertTrue(hasPaint(image, 23, 34));
        assertTrue(hasPaint(image, 11, 23));
    }

    private static boolean hasPaint(BufferedImage image, int fromX, int toX) {
        for (int x = fromX; x < toX; x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
