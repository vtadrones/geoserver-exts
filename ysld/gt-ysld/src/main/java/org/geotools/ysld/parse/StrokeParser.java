package org.geotools.ysld.parse;

import org.geotools.styling.Graphic;
import org.geotools.styling.Stroke;
import org.geotools.ysld.YamlMap;
import org.geotools.ysld.YamlObject;

public abstract class StrokeParser extends YsldParseHandler {
    Stroke stroke;

    protected StrokeParser(Factory factory) {
        super(factory);
    }

    @Override
    public void handle(YamlObject<?> obj, YamlParseContext context) {
        YamlMap map = obj.map();

        if (map.has("stroke-color")) {
            stroke().setColor(Util.color(map.str("stroke-color"), factory));
        }
        if (map.has("stroke-width")) {
            stroke().setWidth(Util.expression(map.str("stroke-width"), factory));
        }
        if (map.has("stroke-opacity")) {
            stroke().setOpacity(Util.expression(map.str("stroke-opacity"), factory));
        }
        if (map.has("stroke-linejoin")) {
            stroke().setLineJoin(Util.expression(map.str("stroke-linejoin"), factory));
        }
        if (map.has("stroke-linecap")) {
            stroke().setLineCap(Util.expression(map.str("stroke-linecap"), factory));
        }
        if (map.has("stroke-dasharray")) {
            stroke().setDashArray(Util.floatArray(map.str("stroke-dasharray")));
        }
        if (map.has("stroke-dashoffset")) {
            stroke().setDashOffset(Util.expression(map.str("stroke-dashoffset"), factory));
        }

        context.push("stroke-graphic", new GraphicParser(factory) {
            @Override
            protected void graphic(Graphic g) {
                stroke().setGraphicFill(g);
            }
        });
        context.push("stroke-graphic-stroke", new GraphicParser(factory) {
            @Override
            protected void graphic(Graphic g) {
                stroke().setGraphicStroke(g);
            }
        });
    }

    Stroke stroke() {
        if (stroke == null) {
            stroke = factory.style.createStroke(null, null);
            stroke(stroke);
        }
        return stroke;
    }

    protected abstract void stroke(Stroke stroke);
}
