package de.ids_mannheim.korap.query.serialize;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author hanl
 * @date 07/02/2014
 */
public class MetaQueryBuilder {

    private Map meta;
    private SpanContext spanContext;

    public MetaQueryBuilder () {
        this.meta = new LinkedHashMap();
    }

    /**
     * context segment if context is either of type char or token.
     * size can differ for left and right span
     *
     * @param left
     * @param leftType
     * @param right
     * @param rightType
     * @return
     */
    public MetaQueryBuilder setSpanContext(Integer left, String leftType,
            Integer right, String rightType) {
        this.spanContext = new SpanContext(left, leftType, right, rightType);
        return this;
    }

    public SpanContext getSpanContext() {
        return this.spanContext;
    }

    /**
     * context if of type paragraph or sentence where left and right
     * size delimiters are irrelevant; or 2-token, 2-char p/paragraph,
     * s/sentence or token, char
     *
     * @param context
     * @return
     */
    public MetaQueryBuilder setSpanContext(String context) {
        if (context.startsWith("s") | context.startsWith("p"))
            this.spanContext = new SpanContext(context);
        else {
            String[] ct = context.replaceAll("\\s+", "").split(",");
            String[] lc = ct[0].split("-");
            String[] rc = ct[1].split("-");
            this.spanContext = new SpanContext(Integer.valueOf(lc[0]), lc[1],
                    Integer.valueOf(rc[0]), rc[1]);
        }
        return this;
    }

    public MetaQueryBuilder fillMeta(Integer pageIndex, Integer pageInteger,
            Integer pageLength, String ctx, Boolean cutoff) {
        if (pageIndex != null)
            this.addEntry("startIndex", pageIndex);
        if (pageIndex == null && pageInteger != null)
            this.addEntry("startPage", pageInteger);
        if (pageLength != null)
            this.addEntry("count", pageLength);
        if (ctx != null)
            this.setSpanContext(ctx);
        if (cutoff != null)
            this.addEntry("cutOff", cutoff);
        return this;
    }

    public MetaQueryBuilder addEntry(String name, Object value) {
        meta.put(name, value);
        return this;
    }

    public Map raw() {
        if (this.spanContext != null)
            meta.putAll(this.spanContext.raw());
        return meta;
    }

    @Data
    public class SpanContext {
        private String left_type;
        private String right_type;
        private int left_size;
        private int right_size;
        private String context = null;

        /**
         * context segment if context is either of type char or token.
         * size can differ for left and right span
         *
         * @param ls
         * @param lt
         * @param rs
         * @param rt
         * @return
         */
        public SpanContext (int ls, String lt, int rs, String rt) {
            this.left_type = lt;
            this.left_size = ls;
            this.right_type = rt;
            this.right_size = rs;
        }

        public SpanContext (String context) {
            this.context = context;
        }

        public Map raw() {
            Map meta = new LinkedHashMap();
            if (this.context == null) {
                Map map = new LinkedHashMap();
                List l = new LinkedList();
                List r = new LinkedList();
                l.add(this.left_type);
                l.add(this.left_size);
                map.put("left", l);
                r.add(this.right_type);
                r.add(this.right_size);
                map.put("right", r);
                meta.put("context", map);
            }
            else
                meta.put("context", this.context);
            return meta;
        }
    }
}
