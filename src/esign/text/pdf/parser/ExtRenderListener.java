package esign.text.pdf.parser;

public interface ExtRenderListener extends RenderListener {
  void modifyPath(PathConstructionRenderInfo paramPathConstructionRenderInfo);
  
  Path renderPath(PathPaintingRenderInfo paramPathPaintingRenderInfo);
  
  void clipPath(int paramInt);
}

