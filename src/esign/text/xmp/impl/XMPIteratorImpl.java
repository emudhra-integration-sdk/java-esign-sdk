package esign.text.xmp.impl;

import esign.text.xmp.XMPException;
import esign.text.xmp.XMPIterator;
import esign.text.xmp.XMPMetaFactory;
import esign.text.xmp.impl.xpath.XMPPath;
import esign.text.xmp.impl.xpath.XMPPathParser;
import esign.text.xmp.options.IteratorOptions;
import esign.text.xmp.options.PropertyOptions;
import esign.text.xmp.properties.XMPPropertyInfo;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class XMPIteratorImpl
        implements XMPIterator {

    private IteratorOptions options;
    private String baseNS = null;

    protected boolean skipSiblings = false;

    protected boolean skipSubtree = false;

    private Iterator nodeIterator = null;

    public XMPIteratorImpl(XMPMetaImpl xmp, String schemaNS, String propPath, IteratorOptions options) throws XMPException {
        this.options = (options != null) ? options : new IteratorOptions();

        XMPNode startNode = null;
        String initialPath = null;
        boolean baseSchema = (schemaNS != null && schemaNS.length() > 0);
        boolean baseProperty = (propPath != null && propPath.length() > 0);

        if (!baseSchema && !baseProperty) {

            startNode = xmp.getRoot();
        } else if (baseSchema && baseProperty) {

            XMPPath path = XMPPathParser.expandXPath(schemaNS, propPath);

            XMPPath basePath = new XMPPath();
            for (int i = 0; i < path.size() - 1; i++) {
                basePath.add(path.getSegment(i));
            }

            startNode = XMPNodeUtils.findNode(xmp.getRoot(), path, false, null);
            this.baseNS = schemaNS;
            initialPath = basePath.toString();
        } else if (baseSchema && !baseProperty) {

            startNode = XMPNodeUtils.findSchemaNode(xmp.getRoot(), schemaNS, false);

        } else {

            throw new XMPException("Schema namespace URI is required", 101);
        }

        if (startNode != null) {

            if (!this.options.isJustChildren()) {
                this.nodeIterator = new NodeIterator(startNode, initialPath, 1);
            } else {
                this.nodeIterator = new NodeIteratorChildren(startNode, initialPath);
            }

        } else {

            this.nodeIterator = Collections.EMPTY_LIST.iterator();
        }
    }

    public void skipSubtree() {
        this.skipSubtree = true;
    }

    public void skipSiblings() {
        skipSubtree();
        this.skipSiblings = true;
    }

    public boolean hasNext() {
        return this.nodeIterator.hasNext();
    }

    public Object next() {
        return this.nodeIterator.next();
    }

    public void remove() {
        throw new UnsupportedOperationException("The XMPIterator does not support remove().");
    }

    protected IteratorOptions getOptions() {
        return this.options;
    }

    protected String getBaseNS() {
        return this.baseNS;
    }

    protected void setBaseNS(String baseNS) {
        this.baseNS = baseNS;
    }

    private class NodeIterator
            implements Iterator {

        protected static final int ITERATE_NODE = 0;

        protected static final int ITERATE_CHILDREN = 1;

        protected static final int ITERATE_QUALIFIER = 2;

        private int state = 0;

        private XMPNode visitedNode;

        private String path;

        private Iterator childrenIterator = null;

        private int index = 0;

        private Iterator subIterator = Collections.EMPTY_LIST.iterator();

        private XMPPropertyInfo returnProperty = null;

        public NodeIterator() {
        }

        public NodeIterator(XMPNode visitedNode, String parentPath, int index) {
            this.visitedNode = visitedNode;
            this.state = 0;
            if (visitedNode.getOptions().isSchemaNode()) {
                XMPIteratorImpl.this.setBaseNS(visitedNode.getName());
            }

            this.path = accumulatePath(visitedNode, parentPath, index);
        }

        public boolean hasNext() {
            if (this.returnProperty != null) {

                return true;
            }

            if (this.state == 0) {
                return reportNode();
            }
            if (this.state == 1) {

                if (this.childrenIterator == null) {
                    this.childrenIterator = this.visitedNode.iterateChildren();
                }

                boolean hasNext = iterateChildren(this.childrenIterator);

                if (!hasNext && this.visitedNode.hasQualifier() && !XMPIteratorImpl.this.getOptions().isOmitQualifiers()) {

                    this.state = 2;
                    this.childrenIterator = null;
                    hasNext = hasNext();
                }
                return hasNext;
            }

            if (this.childrenIterator == null) {
                this.childrenIterator = this.visitedNode.iterateQualifier();
            }

            return iterateChildren(this.childrenIterator);
        }

        protected boolean reportNode() {
            this.state = 1;
            if (this.visitedNode.getParent() != null && (!XMPIteratorImpl.this.getOptions().isJustLeafnodes() || !this.visitedNode.hasChildren())) {

                this.returnProperty = createPropertyInfo(this.visitedNode, XMPIteratorImpl.this.getBaseNS(), this.path);
                return true;
            }

            return hasNext();
        }

        private boolean iterateChildren(Iterator<XMPNode> iterator) {
            if (XMPIteratorImpl.this.skipSiblings) {

                XMPIteratorImpl.this.skipSiblings = false;
                this.subIterator = Collections.EMPTY_LIST.iterator();
            }

            if (!this.subIterator.hasNext() && iterator.hasNext()) {

                XMPNode child = iterator.next();
                this.index++;
                this.subIterator = new NodeIterator(child, this.path, this.index);
            }

            if (this.subIterator.hasNext()) {

                this.returnProperty = (XMPPropertyInfo)this.subIterator.next();
                return true;
            }

            return false;
        }

        public Object next() {
            if (hasNext()) {

                XMPPropertyInfo result = this.returnProperty;
                this.returnProperty = null;
                return result;
            }

            throw new NoSuchElementException("There are no more nodes to return");
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        protected String accumulatePath(XMPNode currNode, String parentPath, int currentIndex) {
            String separator;
            String segmentName;
            if (currNode.getParent() == null || currNode.getOptions().isSchemaNode()) {
                return null;
            }
            if (currNode.getParent().getOptions().isArray()) {

                separator = "";
                segmentName = "[" + String.valueOf(currentIndex) + "]";
            } else {

                separator = "/";
                segmentName = currNode.getName();
            }

            if (parentPath == null || parentPath.length() == 0) {
                return segmentName;
            }
            if (XMPIteratorImpl.this.getOptions().isJustLeafname()) {
                return !segmentName.startsWith("?") ? segmentName : segmentName
                        .substring(1);
            }

            return parentPath + separator + segmentName;
        }

        protected XMPPropertyInfo createPropertyInfo(final XMPNode node, final String baseNS, final String path) {
            final String value = node.getOptions().isSchemaNode() ? null : node.getValue();

            return new XMPPropertyInfo() {
                public String getNamespace() {
                    if (!node.getOptions().isSchemaNode()) {

                        QName qname = new QName(node.getName());
                        return XMPMetaFactory.getSchemaRegistry().getNamespaceURI(qname.getPrefix());
                    }

                    return baseNS;
                }

                public String getPath() {
                    return path;
                }

                public String getValue() {
                    return value;
                }

                public PropertyOptions getOptions() {
                    return node.getOptions();
                }

                public String getLanguage() {
                    return null;
                }
            };
        }

        protected Iterator getChildrenIterator() {
            return this.childrenIterator;
        }

        protected void setChildrenIterator(Iterator childrenIterator) {
            this.childrenIterator = childrenIterator;
        }

        protected XMPPropertyInfo getReturnProperty() {
            return this.returnProperty;
        }

        protected void setReturnProperty(XMPPropertyInfo returnProperty) {
            this.returnProperty = returnProperty;
        }
    }

    private class NodeIteratorChildren
            extends NodeIterator {

        private String parentPath;

        private Iterator childrenIterator;

        private int index = 0;

        public NodeIteratorChildren(XMPNode parentNode, String parentPath) {
            if (parentNode.getOptions().isSchemaNode()) {
                XMPIteratorImpl.this.setBaseNS(parentNode.getName());
            }
            this.parentPath = accumulatePath(parentNode, parentPath, 1);

            this.childrenIterator = parentNode.iterateChildren();
        }

        public boolean hasNext() {
            if (getReturnProperty() != null) {

                return true;
            }
            if (XMPIteratorImpl.this.skipSiblings) {
                return false;
            }
            if (this.childrenIterator.hasNext()) {

                XMPNode child = (XMPNode)this.childrenIterator.next();
                this.index++;

                String path = null;
                if (child.getOptions().isSchemaNode()) {

                    XMPIteratorImpl.this.setBaseNS(child.getName());
                } else if (child.getParent() != null) {

                    path = accumulatePath(child, this.parentPath, this.index);
                }

                if (!XMPIteratorImpl.this.getOptions().isJustLeafnodes() || !child.hasChildren()) {

                    setReturnProperty(createPropertyInfo(child, XMPIteratorImpl.this.getBaseNS(), path));
                    return true;
                }

                return hasNext();
            }

            return false;
        }
    }
}

