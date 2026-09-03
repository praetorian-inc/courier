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

package burp;

import burp.api.montoya.MontoyaApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import javax.imageio.ImageIO;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CourierConnectorTest {
    @TempDir
    Path temporaryHome;

    @Test
    void initializesAndUnloadsCompleteExtensionRuntime() throws Exception {
        String originalHome = System.getProperty("user.home");
        System.setProperty("user.home", temporaryHome.toString());
        try {
            if ("true".equalsIgnoreCase(System.getenv("COURIER_DARK_SCREENSHOT"))) {
                UIManager.put("Panel.background", new Color(45, 47, 49));
                UIManager.put("Label.foreground", new Color(230, 230, 230));
            }
            MontoyaApi api = mock(MontoyaApi.class, RETURNS_DEEP_STUBS);
            CourierConnector connector = new CourierConnector();

            assertDoesNotThrow(() -> connector.initialize(api));
            verify(api.extension()).setName("Courier");
            JPanel panel = extensionPanel(connector);
            assertDoesNotThrow(() -> SwingUtilities.invokeAndWait(() -> {
                panel.setSize(1400, 900);
                String selectedTab = System.getenv("COURIER_SCREENSHOT_TAB");
                JTabbedPane tabs = findTabs(panel);
                if (tabs != null && selectedTab != null && !selectedTab.isBlank()) {
                    tabs.setSelectedIndex(Integer.parseInt(selectedTab));
                }
                layoutTree(panel);
                BufferedImage image = new BufferedImage(1400, 900, BufferedImage.TYPE_INT_RGB);
                var graphics = image.createGraphics();
                try {
                    panel.printAll(graphics);
                } finally {
                    graphics.dispose();
                }
                String screenshotPath = System.getenv("COURIER_SCREENSHOT");
                if (screenshotPath != null && !screenshotPath.isBlank()) {
                    try {
                        ImageIO.write(image, "png", new File(screenshotPath));
                    } catch (Exception exception) {
                        throw new IllegalStateException(exception);
                    }
                }
            }));
            assertDoesNotThrow(connector::extensionUnloaded);
            SwingUtilities.invokeAndWait(() -> { });
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    private static JTabbedPane findTabs(Container container) {
        for (var component : container.getComponents()) {
            if (component instanceof JTabbedPane tabs) {
                return tabs;
            }
            if (component instanceof Container child) {
                JTabbedPane tabs = findTabs(child);
                if (tabs != null) {
                    return tabs;
                }
            }
        }
        return null;
    }

    private static void layoutTree(Container container) {
        container.doLayout();
        for (var component : container.getComponents()) {
            if (component instanceof Container child) {
                layoutTree(child);
            }
        }
    }

    private static JPanel extensionPanel(CourierConnector connector) throws Exception {
        Field field = CourierConnector.class.getDeclaredField("panel");
        field.setAccessible(true);
        return (JPanel) field.get(connector);
    }
}
