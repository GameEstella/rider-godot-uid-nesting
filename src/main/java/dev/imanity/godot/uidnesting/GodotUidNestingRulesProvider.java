package dev.imanity.godot.uidnesting;

import com.intellij.ide.projectView.ProjectViewNestingRulesProvider;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class GodotUidNestingRulesProvider implements ProjectViewNestingRulesProvider {
  private static final String UID_SUFFIX = ".uid";
  private static final String TRANSLATION_SUFFIX = ".translation";

  // Rider's File System pane uses file nesting rules by suffix, so we register
  // a broad set of common Godot and asset extensions here.
  private static final String[] UID_PARENT_SUFFIXES = {
    ".gd",
    ".cs",
    ".txt",
    ".cfg",
    ".ini",
    ".json",
    ".xml",
    ".csv",
    ".md",
    ".yml",
    ".yaml",
    ".toml",
    ".tscn",
    ".tres",
    ".res",
    ".gdshader",
    ".gdshaderinc",
    ".shader",
    ".glsl",
    ".wgsl",
    ".gdextension",
    ".png",
    ".jpg",
    ".jpeg",
    ".webp",
    ".svg",
    ".bmp",
    ".gif",
    ".ico",
    ".ttf",
    ".otf",
    ".woff",
    ".woff2",
    ".wav",
    ".mp3",
    ".ogg",
    ".flac",
    ".ogv",
    ".mp4",
    ".mov",
    ".mkv",
    ".avi",
    ".gltf",
    ".glb",
    ".obj",
    ".dae",
    ".fbx",
    ".blend",
    ".bin",
    ".dat",
    ".proto",
    ".sln",
  };

  private static final String[] TRANSLATION_PARENT_SUFFIXES = {
    ".csv",
    ".tsv",
    ".txt",
    ".cfg",
    ".ini",
    ".json",
    ".xml",
    ".yml",
    ".yaml",
    ".toml",
    ".po",
    ".pot",
  };

  @Override
  public void addFileNestingRules(@NotNull Consumer consumer) {
    for (String parentSuffix : UID_PARENT_SUFFIXES) {
      consumer.addNestingRule(parentSuffix, parentSuffix + UID_SUFFIX);
    }

    for (String parentSuffix : TRANSLATION_PARENT_SUFFIXES) {
      for (String localeTag : buildTranslationLocaleTags()) {
        consumer.addNestingRule(parentSuffix, "." + localeTag + TRANSLATION_SUFFIX);
      }
    }
  }

  private static @NotNull Set<String> buildTranslationLocaleTags() {
    LinkedHashSet<String> tags = new LinkedHashSet<>();

    for (Locale locale : Locale.getAvailableLocales()) {
      String language = locale.getLanguage();
      if (language == null || language.isBlank()) {
        continue;
      }

      tags.add(language);

      String script = locale.getScript();
      String country = locale.getCountry();

      if (script != null && !script.isBlank()) {
        tags.add(language + "_" + script);
        tags.add(language + "-" + script);
      }

      if (country != null && !country.isBlank()) {
        tags.add(language + "_" + country);
        tags.add(language + "-" + country);
      }

      if (script != null && !script.isBlank() && country != null && !country.isBlank()) {
        tags.add(language + "_" + script + "_" + country);
        tags.add(language + "-" + script + "-" + country);
      }
    }

    return tags;
  }
}
