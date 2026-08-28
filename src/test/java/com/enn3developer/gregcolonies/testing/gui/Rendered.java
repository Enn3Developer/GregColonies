package com.enn3developer.gregcolonies.testing.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.PanelManager;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

public final class Rendered {

    public static final class Node {

        private final IWidget widget;

        private final int depth;

        private final String text;

        private Node(IWidget widget, int depth, String text) {
            this.widget = widget;
            this.depth = depth;
            this.text = text;
        }

        public IWidget widget() {
            return widget;
        }

        public String name() {
            return widget.getName();
        }

        public Class<?> type() {
            return widget.getClass();
        }

        public int depth() {
            return depth;
        }

        public String text() {
            return text;
        }

        public String label() {
            return Rendered.label(widget);
        }

        public boolean hasText() {
            return text != null && !text.isEmpty();
        }

        public Area area() {
            return widget.getArea();
        }

        public int x() {
            return widget.getArea()
                .x();
        }

        public int y() {
            return widget.getArea()
                .y();
        }

        public int width() {
            return widget.getArea()
                .w();
        }

        public int height() {
            return widget.getArea()
                .h();
        }

        public int centerX() {
            return x() + width() / 2;
        }

        public int centerY() {
            return y() + height() / 2;
        }

        public boolean enabled() {
            for (IWidget current = widget; current != null; current = current.getParent()) {
                if (!current.isEnabled()) {
                    return false;
                }
            }
            return true;
        }

        public boolean contains(int x, int y) {
            return x >= x() && x < x() + width() && y >= y() && y < y() + height();
        }

        @Override
        public String toString() {
            String label = text == null ? "" : " \"" + text + "\"";
            return type().getSimpleName() + "["
                + x()
                + ","
                + y()
                + " "
                + width()
                + "x"
                + height()
                + (enabled() ? "" : " disabled")
                + "]"
                + label;
        }
    }

    private final List<Node> nodes;

    private Rendered(List<Node> nodes) {
        this.nodes = Collections.unmodifiableList(nodes);
    }

    public static Rendered of(ModularScreen screen) {
        List<Node> nodes = new ArrayList<>();
        for (ModularPanel panel : panels(screen)) {
            collect(panel, 0, nodes);
        }
        return new Rendered(nodes);
    }

    public List<Node> all() {
        return nodes;
    }

    public List<Node> visible() {
        return nodes.stream()
            .filter(Node::enabled)
            .filter(node -> node.width() > 0 && node.height() > 0)
            .collect(Collectors.toList());
    }

    public List<String> texts() {
        return visible().stream()
            .filter(Node::hasText)
            .map(Node::text)
            .collect(Collectors.toList());
    }

    public boolean shows(String text) {
        return !withText(text).isEmpty();
    }

    public List<Node> withText(String text) {
        return visible().stream()
            .filter(node -> text.equals(node.text()))
            .collect(Collectors.toList());
    }

    public List<Node> containingText(String part) {
        return visible().stream()
            .filter(
                node -> node.hasText() && node.text()
                    .contains(part))
            .collect(Collectors.toList());
    }

    public Node byText(String text) {
        List<Node> found = withText(text);
        if (found.size() != 1) {
            throw new AssertionError(
                "expected exactly one widget showing \"" + text + "\" but found " + found.size() + "\n" + this);
        }
        return found.get(0);
    }

    public List<Node> buttons() {
        return ofType(ButtonWidget.class);
    }

    public List<Node> buttons(String label) {
        return buttons().stream()
            .filter(node -> label.equals(node.label()))
            .collect(Collectors.toList());
    }

    public Node button(String label) {
        List<Node> found = buttons(label);
        if (found.size() != 1) {
            throw new AssertionError(
                "expected exactly one button labelled \"" + label + "\" but found " + found.size() + "\n" + this);
        }
        return found.get(0);
    }

    public List<Node> onScreen(int width, int height) {
        return visible().stream()
            .filter(node -> node.x() >= 0 && node.y() >= 0)
            .filter(node -> node.x() + node.width() <= width && node.y() + node.height() <= height)
            .collect(Collectors.toList());
    }

    public List<Node> ofType(Class<?> type) {
        return visible().stream()
            .filter(node -> type.isInstance(node.widget()))
            .collect(Collectors.toList());
    }

    public Node named(String name) {
        for (Node node : visible()) {
            if (name.equals(node.name())) {
                return node;
            }
        }
        throw new AssertionError("no widget named \"" + name + "\"\n" + this);
    }

    public Node at(int x, int y) {
        Node hit = null;
        for (Node node : visible()) {
            if (node.contains(x, y)) {
                hit = node;
            }
        }
        return hit;
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        for (Node node : nodes) {
            for (int indent = 0; indent < node.depth(); indent++) {
                out.append("  ");
            }
            out.append(node)
                .append('\n');
        }
        return out.toString();
    }

    private static void collect(IWidget widget, int depth, List<Node> nodes) {
        nodes.add(new Node(widget, depth, textOf(widget)));
        for (IWidget child : widget.getChildren()) {
            collect(child, depth + 1, nodes);
        }
    }

    private static String label(IWidget widget) {
        String own = textOf(widget);
        if (own != null && !own.isEmpty()) {
            return own;
        }
        for (IWidget child : widget.getChildren()) {
            String found = label(child);
            if (found != null && !found.isEmpty()) {
                return found;
            }
        }
        return null;
    }

    private static String textOf(IWidget widget) {
        if (widget instanceof TextWidget) {
            return ((TextWidget<?>) widget).getKey()
                .get();
        }
        if (widget instanceof TextFieldWidget) {
            return ((TextFieldWidget) widget).getText();
        }
        return null;
    }

    private static List<ModularPanel> panels(ModularScreen screen) {
        PanelManager manager = screen.getPanelManager();
        List<ModularPanel> open = manager.getOpenPanels();
        return open == null || open.isEmpty() ? Collections.singletonList(manager.getMainPanel())
            : new ArrayList<>(open);
    }
}
