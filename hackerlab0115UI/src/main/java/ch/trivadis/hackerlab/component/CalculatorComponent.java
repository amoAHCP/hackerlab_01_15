/************************************************************************
 *
 * Copyright (C) 2010 - 2012
 *
 * [ComponentLeft.java]
 * AHCP Project (http://jacp.googlecode.com)
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 *
 *     http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either 
 * express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 *
 *
 ************************************************************************/
package ch.trivadis.hackerlab.component;

import ch.trivadis.hackerlab.config.BasicConfig;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import org.jacpfx.api.annotations.Resource;
import org.jacpfx.api.annotations.component.View;
import org.jacpfx.api.annotations.lifecycle.PostConstruct;
import org.jacpfx.api.annotations.lifecycle.PreDestroy;
import org.jacpfx.api.message.Message;
import org.jacpfx.rcp.component.FXComponent;
import org.jacpfx.rcp.componentLayout.FXComponentLayout;
import org.jacpfx.rcp.context.Context;
import org.jacpfx.rcp.util.FXUtil;
import org.jacpfx.rcp.util.LayoutUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static javafx.scene.layout.Priority.ALWAYS;

/**
 * A simple JacpFX UI component
 *
 * @author Andy Moncsek
 */
@View(id = BasicConfig.COMPONENT_CALCULATOR,
        name = "SimpleView",
        active = true,
        resourceBundleLocation = "bundles.languageBundle",
        initialTargetLayoutId = BasicConfig.TARGET_CONTAINER_LEFT)
public class CalculatorComponent implements FXComponent {
    private Node pane;
    private Logger log = Logger.getLogger(CalculatorComponent.class.getName());
    @Resource
    private Context context;
    private static final String[][] template = {
            {"7", "8", "9", "/"},
            {"4", "5", "6", "*"},
            {"1", "2", "3", "-"},
            {"0", "c", "=", "+"}
    };

    private final Map<String, Button> accelerators = new HashMap<>();

    private DoubleProperty stackValue = new SimpleDoubleProperty();
    private DoubleProperty value = new SimpleDoubleProperty();

    private enum Op {NOOP, ADD, SUBTRACT, MULTIPLY, DIVIDE}

    private Op curOp = Op.NOOP;
    private Op stackOp = Op.NOOP;

    @Override
    /**
     * The handle method always runs outside the main application thread. You can create new nodes, execute long running tasks but you are not allowed to manipulate existing nodes here.
     */
    public Node handle(final Message<Event, Object> message) {
        // runs in worker thread

        return null;
    }

    @Override
    /**
     * The postHandle method runs always in the main application thread.
     */
    public Node postHandle(final Node arg0,
                           final Message<Event, Object> message) {
        // runs in FX application thread
        if (message.messageBodyEquals(FXUtil.MessageUtil.INIT)) {

        }
        return this.pane;
    }

    @PostConstruct
    /**
     * The @PostConstruct annotation labels methods executed when the component switch from inactive to active state
     * @param arg0
     * @param resourceBundle
     */
    public void onPostConstructComponent(final FXComponentLayout arg0,
                                         final ResourceBundle resourceBundle) {
        this.pane = createUI();
        this.log.info("run on start of ComponentLeft ");
    }

    @PreDestroy
    /**
     * The @PreDestroy annotations labels methods executed when the component is set to inactive
     * @param arg0
     */
    public void onPreDestroyComponent(final FXComponentLayout arg0) {
        this.log.info("run on tear down of ComponentLeft ");

    }

    /**
     * create the UI on first call
     *
     * @return
     */
    private Node createUI() {
        final TextField screen = createScreen();
        final TilePane buttons = createButtons();
        LayoutUtil.GridPaneUtil.setFullGrow(ALWAYS, screen);
        LayoutUtil.GridPaneUtil.setFullGrow(ALWAYS, buttons);
        return createLayout(screen, buttons);
    }

    private VBox createLayout(TextField screen, TilePane buttons) {
        final VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: beige; -fx-padding: 20; -fx-font-size: 20;");
        layout.getChildren().setAll(screen, buttons);
        handleAccelerators(layout);
        screen.prefWidthProperty().bind(buttons.widthProperty());
        // let them grow
        LayoutUtil.GridPaneUtil.setFullGrow(ALWAYS, layout);
        return layout;
    }

    private void handleAccelerators(VBox layout) {
        layout.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
            Button activated = accelerators.get(keyEvent.getText());
            if (activated != null) {
                activated.fire();
            }
        });

    }

    private TextField createScreen() {
        final TextField screen = new TextField();
        screen.setStyle("-fx-background-color: aquamarine;");
        screen.setAlignment(Pos.CENTER_RIGHT);
        screen.setEditable(false);
        screen.textProperty().bind(Bindings.format("%.0f", value));
        return screen;
    }

    private TilePane createButtons() {
        TilePane buttons = new TilePane();
        buttons.setVgap(10);
        buttons.setHgap(15);
        buttons.setPrefColumns(template[0].length);
        for (String[] r : template) {
            for (String s : r) {
                buttons.getChildren().add(createButton(s));
            }
        }
        return buttons;
    }

    private Button createButton(final String s) {
        Button button = makeStandardButton(s);

        if (s.matches("[0-9]")) {
            makeNumericButton(s, button);
        } else {
            final ObjectProperty<Op> triggerOp = determineOperand(s);
            if (triggerOp.get() != Op.NOOP) {
                makeOperandButton(button, triggerOp);
            } else if ("c".equals(s)) {
                makeClearButton(button);
            } else if ("=".equals(s)) {
                makeEqualsButton(button);
            }
        }

        return button;
    }

    private ObjectProperty<Op> determineOperand(String s) {
        final ObjectProperty<Op> triggerOp = new SimpleObjectProperty<>(Op.NOOP);
        switch (s) {
            case "+":
                triggerOp.set(Op.ADD);
                break;
            case "-":
                triggerOp.set(Op.SUBTRACT);
                break;
            case "*":
                triggerOp.set(Op.MULTIPLY);
                break;
            case "/":
                triggerOp.set(Op.DIVIDE);
                break;
        }
        return triggerOp;
    }

    private void makeOperandButton(Button button, final ObjectProperty<Op> triggerOp) {
        button.setStyle("-fx-base: lightgray;");
        button.setOnAction(actionEvent ->
                        curOp = triggerOp.get()
        );
    }

    private Button makeStandardButton(String s) {
        Button button = new Button(s);
        button.setStyle("-fx-base: beige;");
        accelerators.put(s, button);
        button.setPrefWidth(55);
        return button;
    }

    private void makeNumericButton(final String s, Button button) {
        button.setOnAction(actionEvent -> {
            if (curOp == Op.NOOP) {
                value.set(value.get() * 10 + Integer.parseInt(s));
            } else {
                stackValue.set(value.get());
                value.set(Integer.parseInt(s));
                stackOp = curOp;
                curOp = Op.NOOP;
            }
        });
    }

    private void makeClearButton(Button button) {
        button.setStyle("-fx-base: mistyrose;");
        button.setOnAction(actionEvent ->
                        value.set(0)
        );
    }

    private void makeEqualsButton(Button button) {
        button.setStyle("-fx-base: ghostwhite;");
        button.setOnAction(actionEvent -> {
            switch (stackOp) {
                case ADD:
                    value.set(stackValue.get() + value.get());
                    break;
                case SUBTRACT:
                    value.set(stackValue.get() - value.get());
                    break;
                case MULTIPLY:
                    value.set(stackValue.get() * value.get());
                    break;
                case DIVIDE:
                    value.set(stackValue.get() / value.get());
                    break;
            }
        });
    }


}
