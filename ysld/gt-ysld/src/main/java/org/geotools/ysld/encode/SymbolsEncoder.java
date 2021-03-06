package org.geotools.ysld.encode;

import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.opengis.metadata.citation.OnLineResource;
import org.opengis.style.GraphicalSymbol;

public class SymbolsEncoder extends YsldEncodeHandler<GraphicalSymbol> {

    public SymbolsEncoder(Graphic g) {
        super(g.graphicalSymbols().iterator());
    }

    @Override
    protected void encode(GraphicalSymbol symbol) {
        if (symbol instanceof Mark) {
            push("mark");
            encode((Mark) symbol);
        }
        else if (symbol instanceof ExternalGraphic) {
            push("external");
            encode((ExternalGraphic) symbol);
        }
    }

    SymbolsEncoder encode(Mark mark) {
        putName("shape", mark.getWellKnownName());
        inline(new StrokeEncoder(mark.getStroke()));
        inline(new FillEncoder(mark.getFill()));
        //encode("stroke", new StrokeEncoder(mark.getStroke()));
        //encode("fill", mark.getFill());
        //url:
        //inline:
        return this;
    }

    SymbolsEncoder encode(ExternalGraphic eg) {
        OnLineResource r = eg.getOnlineResource();
        if (r != null) {
            put("url", r.getLinkage().toString());
        }

        put("format", eg.getFormat());
        return this;
    }
}
