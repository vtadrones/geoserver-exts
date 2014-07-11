package org.geotools.ysld.parse;

import org.geotools.styling.*;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.MappingEndEvent;
import org.yaml.snakeyaml.events.ScalarEvent;

import java.util.Deque;

public class TextHandler extends SymbolizerHandler<TextSymbolizer> {

    public TextHandler(Rule rule, Factory factory) {
        super(rule, factory.style.createTextSymbolizer(), factory);
    }

    @Override
    public void scalar(ScalarEvent evt, Deque<YamlParseHandler> handlers) {
        String val = evt.getValue();
        if ("label".equals(val)) {
            handlers.push(new ExpressionHandler(factory) {
                @Override
                protected void expression(Expression expr) {
                    sym.setLabel(expr);
                }
            });
        }
        else if ("font".equals(val)) {
            handlers.push(new FontHandler());
        }
        else if ("fill".equals(val)) {
            handlers.push(new FillHandler(factory) {
                @Override
                protected void fill(Fill fill) {
                    sym.setFill(fill);
                }
            });
        }
        else if ("halo".equals(val)) {
            handlers.push(new HaloHandler());
        }
        else if ("placement".equals(val)) {
            handlers.push(new PlacementHandler());
        }
        else {
            super.scalar(evt, handlers);
        }
    }

    class FontHandler extends YsldParseHandler {

        Font font;

        protected FontHandler() {
            super(TextHandler.this.factory);

            FilterFactory ff = factory.filter;
            sym.setFont(font = factory.style.createFont(
                ff.literal("serif"), ff.literal("normal"), ff.literal("normal"), ff.literal(10)));
        }

        @Override
        public void scalar(ScalarEvent evt, Deque<YamlParseHandler> handlers) {
            String val = evt.getValue();
            if ("family".equals(val)) {
                handlers.push(new ExpressionHandler(factory) {
                    @Override
                    protected void expression(Expression expr) {
                        font.setFontFamily(expr);
                    }
                });
            }
            else if ("size".equals(val)) {
                handlers.push(new ExpressionHandler(factory) {
                    @Override
                    protected void expression(Expression expr) {
                        font.setSize(expr);
                    }
                });
            }
            else if ("style".equals(val)) {
                handlers.push(new ExpressionHandler(factory) {
                    @Override
                    protected void expression(Expression expr) {
                        font.setStyle(expr);
                    }
                });
            }
            else if ("weight".equals(val)) {
                handlers.push(new ExpressionHandler(factory) {
                    @Override
                    protected void expression(Expression expr) {
                        font.setWeight(expr);
                    }
                });
            }
        }

        @Override
        public void endMapping(MappingEndEvent evt, Deque<YamlParseHandler> handlers) {
            super.endMapping(evt, handlers);
            handlers.pop();
        }
    }

    class HaloHandler extends YsldParseHandler {

        Halo halo;
        HaloHandler() {
            super(TextHandler.this.factory);
            sym.setHalo(halo = this.factory.style.createHalo(null, null));
        }

        @Override
        public void scalar(ScalarEvent evt, Deque<YamlParseHandler> handlers) {
            String val = evt.getValue();
            if ("fill".equals(val)) {
                handlers.push(new FillHandler(factory) {
                    @Override
                    protected void fill(Fill fill) {
                        halo.setFill(fill);
                    }
                });
            }
            else if ("radius".equals(val)) {
                handlers.push(new ExpressionHandler(factory) {
                    @Override
                    protected void expression(Expression expr) {
                        halo.setRadius(expr);
                    }
                });
            }
        }

        @Override
        public void endMapping(MappingEndEvent evt, Deque<YamlParseHandler> handlers) {
            super.endMapping(evt, handlers);
            handlers.pop();
        }
    }

    class PlacementHandler extends YsldParseHandler {

        String type;
        PointPlacement point;
        LinePlacement line;

        protected PlacementHandler() {
            super(TextHandler.this.factory);
            point = factory.style.createPointPlacement(null, null, null);
            line = factory.style.createLinePlacement(null);
        }

        @Override
        public void scalar(ScalarEvent evt, Deque<YamlParseHandler> handlers) {
            String val = evt.getValue();
            if ("type".equals(val)) {
                handlers.push(new ValueHandler(factory) {
                    @Override
                    protected void value(String value, Event event) {
                        type = value;
                    }
                });
            }
            else if ("offset".equals(val)) {
                handlers.push(new ExpressionHandler(factory) {
                    @Override
                    protected void expression(Expression expr) {
                        line.setPerpendicularOffset(expr);
                    }
                });
            }
            else if ("anchor".equals(val)) {
                handlers.push(new AnchorHandler(factory) {
                    @Override
                    protected void anchor(AnchorPoint anchor) {
                        point.setAnchorPoint(anchor);
                    }
                });
            }
            else if ("displacement".equals(val)) {
                handlers.push(new DisplacementHandler(factory) {
                    @Override
                    protected void displace(Displacement displacement) {
                        point.setDisplacement(displacement);
                    }
                });
            }
            else if ("rotation".equals(val)) {
                handlers.push(new ExpressionHandler(factory) {
                    @Override
                    protected void expression(Expression expr) {
                        point.setRotation(expr);
                    }
                });
            }
        }

        @Override
        public void endMapping(MappingEndEvent evt, Deque<YamlParseHandler> handlers) {
            sym.setLabelPlacement("line".equals(type)?line:point);
            handlers.pop();
            super.endMapping(evt, handlers);
        }
    }
}