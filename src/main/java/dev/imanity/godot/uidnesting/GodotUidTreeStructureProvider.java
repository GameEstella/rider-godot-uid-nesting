package dev.imanity.godot.uidnesting;

import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.FileNodeWithNestedFileNodes;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class GodotUidTreeStructureProvider implements TreeStructureProvider, DumbAware {
  private static final String UID_SUFFIX = ".uid";
  private static final String TRANSLATION_SUFFIX = ".translation";
  private static final String GODOT_PROJECT_FILE = "project.godot";

  @Override
  public @NotNull Collection<AbstractTreeNode<?>> modify(@NotNull AbstractTreeNode<?> parent,
                                                         @NotNull Collection<AbstractTreeNode<?>> children,
                                                         ViewSettings settings) {
    if (children.size() < 2) return children;

    Project project = parent.getProject();
    if (project == null) return children;
    if (!(parent instanceof ProjectViewNode<?> parentNode)) return children;

    VirtualFile parentDirectory = parentNode.getVirtualFile();
    if (parentDirectory == null || !parentDirectory.isDirectory()) return children;
    if (!isUnderGodotProject(parentDirectory)) return children;

    Map<String, PsiFileNode> fileNodesByName = new LinkedHashMap<>();
    Map<String, PsiFileNode> fileNodesByStem = new LinkedHashMap<>();
    Set<String> ambiguousStems = new LinkedHashSet<>();
    Map<String, List<PsiFileNode>> uidNodesByParentName = new LinkedHashMap<>();
    Map<String, List<PsiFileNode>> translationNodesByParentStem = new LinkedHashMap<>();

    for (AbstractTreeNode<?> child : children) {
      if (!(child instanceof PsiFileNode fileNode)) continue;

      VirtualFile virtualFile = fileNode.getVirtualFile();
      if (virtualFile == null || virtualFile.isDirectory()) continue;

      String fileName = virtualFile.getName();
      if (isUidSidecar(fileName)) {
        String parentFileName = fileName.substring(0, fileName.length() - UID_SUFFIX.length()).toLowerCase(Locale.ROOT);
        uidNodesByParentName.computeIfAbsent(parentFileName, ignored -> new ArrayList<>(1)).add(fileNode);
      }
      else if (isTranslationSidecar(fileName)) {
        String parentStem = getTranslationParentStem(fileName);
        if (parentStem != null) {
          translationNodesByParentStem.computeIfAbsent(parentStem.toLowerCase(Locale.ROOT), ignored -> new ArrayList<>(1)).add(fileNode);
        }
      }
      else {
        fileNodesByName.putIfAbsent(fileName.toLowerCase(Locale.ROOT), fileNode);

        String stemKey = getFileStem(fileName).toLowerCase(Locale.ROOT);
        if (ambiguousStems.contains(stemKey)) {
          continue;
        }

        PsiFileNode previous = fileNodesByStem.putIfAbsent(stemKey, fileNode);
        if (previous != null && previous != fileNode) {
          ambiguousStems.add(stemKey);
          fileNodesByStem.remove(stemKey);
        }
      }
    }

    if (uidNodesByParentName.isEmpty() && translationNodesByParentStem.isEmpty()) return children;

    Set<PsiFileNode> movedSidecarNodes = new LinkedHashSet<>();
    Map<PsiFileNode, SidecarNestingTreeNode> replacementNodes = new LinkedHashMap<>();

    for (Map.Entry<String, List<PsiFileNode>> entry : uidNodesByParentName.entrySet()) {
      PsiFileNode parentFileNode = fileNodesByName.get(entry.getKey());
      if (parentFileNode == null) continue;

      movedSidecarNodes.addAll(entry.getValue());
      replacementNodes.computeIfAbsent(parentFileNode, SidecarNestingTreeNode::new).addNestedSidecarNodes(entry.getValue());
    }

    for (Map.Entry<String, List<PsiFileNode>> entry : translationNodesByParentStem.entrySet()) {
      PsiFileNode parentFileNode = fileNodesByStem.get(entry.getKey());
      if (parentFileNode == null) continue;

      movedSidecarNodes.addAll(entry.getValue());
      replacementNodes.computeIfAbsent(parentFileNode, SidecarNestingTreeNode::new).addNestedSidecarNodes(entry.getValue());
    }

    if (replacementNodes.isEmpty()) return children;

    ArrayList<AbstractTreeNode<?>> result = new ArrayList<>(children.size() - movedSidecarNodes.size());
    for (AbstractTreeNode<?> child : children) {
      if (child instanceof PsiFileNode fileNode && movedSidecarNodes.contains(fileNode)) {
        continue;
      }

      if (child instanceof PsiFileNode fileNode) {
        SidecarNestingTreeNode replacementNode = replacementNodes.get(fileNode);
        result.add(replacementNode != null ? replacementNode : child);
      }
      else {
        result.add(child);
      }
    }

    return result;
  }

  private static boolean isUnderGodotProject(@NotNull VirtualFile directory) {
    for (VirtualFile current = directory; current != null; current = current.getParent()) {
      if (current.findChild(GODOT_PROJECT_FILE) != null) {
        return true;
      }
    }

    return false;
  }

  private static boolean isUidSidecar(@NotNull String fileName) {
    return fileName.length() > UID_SUFFIX.length()
           && fileName.regionMatches(true, fileName.length() - UID_SUFFIX.length(), UID_SUFFIX, 0, UID_SUFFIX.length());
  }

  private static boolean isTranslationSidecar(@NotNull String fileName) {
    return fileName.length() > TRANSLATION_SUFFIX.length()
           && fileName.regionMatches(true, fileName.length() - TRANSLATION_SUFFIX.length(), TRANSLATION_SUFFIX, 0, TRANSLATION_SUFFIX.length());
  }

  private static String getTranslationParentStem(@NotNull String fileName) {
    if (!isTranslationSidecar(fileName)) return null;

    String withoutSuffix = fileName.substring(0, fileName.length() - TRANSLATION_SUFFIX.length());
    int localeSeparator = withoutSuffix.lastIndexOf('.');
    if (localeSeparator <= 0 || localeSeparator == withoutSuffix.length() - 1) {
      return null;
    }

    return withoutSuffix.substring(0, localeSeparator);
  }

  private static @NotNull String getFileStem(@NotNull String fileName) {
    int extensionSeparator = fileName.lastIndexOf('.');
    if (extensionSeparator <= 0) {
      return fileName;
    }

    return fileName.substring(0, extensionSeparator);
  }

  private static final class SidecarNestingTreeNode extends PsiFileNode implements FileNodeWithNestedFileNodes {
    private final @NotNull PsiFileNode originalNode;
    private final @NotNull Collection<? extends AbstractTreeNode<?>> originalNestedNodes;
    private final @NotNull List<PsiFileNode> nestedSidecarNodes = new ArrayList<>();

    private SidecarNestingTreeNode(@NotNull PsiFileNode originalNode) {
      super(originalNode.getProject(), requirePsiFile(originalNode), originalNode.getSettings());
      this.originalNode = originalNode;
      this.originalNestedNodes = originalNode instanceof FileNodeWithNestedFileNodes nestedFileNode
                                 ? nestedFileNode.getNestedFileNodes()
                                 : Collections.emptyList();
    }

    private static @NotNull PsiFile requirePsiFile(@NotNull PsiFileNode originalNode) {
      PsiFile psiFile = originalNode.getValue();
      if (psiFile == null) {
        throw new IllegalStateException("Cannot create a nesting node for a file node without PSI");
      }
      return psiFile;
    }

    private void addNestedSidecarNodes(@NotNull Collection<PsiFileNode> sidecarNodes) {
      nestedSidecarNodes.addAll(sidecarNodes);
    }

    @Override
    public boolean isAlwaysShowPlus() {
      return true;
    }

    @Override
    public boolean expandOnDoubleClick() {
      return false;
    }

    @Override
    public @NotNull Collection<? extends AbstractTreeNode<?>> getNestedFileNodes() {
      ArrayList<AbstractTreeNode<?>> result = new ArrayList<>(originalNestedNodes.size() + nestedSidecarNodes.size());
      result.addAll(originalNestedNodes);

      for (PsiFileNode node : nestedSidecarNodes) {
        PsiFile psiFile = node.getValue();
        if (psiFile != null) {
          result.add(new PsiFileNode(node.getProject(), psiFile, node.getSettings()));
        }
      }

      return result;
    }

    @Override
    public @NotNull Collection<AbstractTreeNode<?>> getChildrenImpl() {
      // Keep any pre-existing nested nodes, then append the Godot sidecar files.
      ArrayList<AbstractTreeNode<?>> result = new ArrayList<>();
      result.addAll(getNestedFileNodes());

      Set<AbstractTreeNode<?>> originalNestedSet = new LinkedHashSet<>(originalNestedNodes);
      for (AbstractTreeNode<?> child : originalNode.getChildren()) {
        if (!originalNestedSet.contains(child)) {
          result.add(child);
        }
      }

      return result;
    }

    @Override
    public boolean contains(@NotNull VirtualFile file) {
      if (super.contains(file) || originalNode.contains(file)) {
        return true;
      }

      for (PsiFileNode node : nestedSidecarNodes) {
        if (file.equals(node.getVirtualFile())) {
          return true;
        }
      }

      return false;
    }
  }
}
