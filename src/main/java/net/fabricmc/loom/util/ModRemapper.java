/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016, 2017, 2018 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.fabricmc.loom.util;

import com.google.common.io.Files;
import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.providers.MappingsProvider;
import net.fabricmc.loom.task.RemapJar;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModRemapper {
	public static void remap(RemapJar task) {
		remap(task, task.getJar(), task.getDestination(), task.isNestJar(), !task.includeAT);
	}

	public static void remap(Task task, File modJar, File modBackup, boolean nest, boolean skipATs) {
		Project project = task.getProject();

		if (!modJar.exists()) {
			project.getLogger().error("Source .JAR not found!");
			return;
		}

		LoomGradleExtension extension = project.getExtensions().getByType(LoomGradleExtension.class);
		MappingsProvider mappingsProvider = extension.getMappingsProvider();

		Path mappings = mappingsProvider.MAPPINGS_TINY.toPath();

		String fromM = "named";
		String toM = "intermediary";

		List<File> classpathFiles = new ArrayList<>();
		classpathFiles.addAll(project.getConfigurations().getByName(Constants.COMPILE_MODS_MAPPED).getFiles());
		classpathFiles.addAll(project.getConfigurations().getByName(Constants.MINECRAFT_NAMED).getFiles());
		final Path modJarPath = modJar.toPath();
		Path[] classpath = classpathFiles.stream().map(File::toPath).filter((p) -> !modJarPath.equals(p)).toArray(Path[]::new);

		String s = modJar.getAbsolutePath();
		File modJarOutput = new File(s.substring(0, s.length() - 4) + ".remapped.jar");
		Path modJarOutputPath = modJarOutput.toPath();

		File modJarUnmappedCopy = modBackup;
		if (modJarUnmappedCopy.exists()) {
			modJarUnmappedCopy.delete();
		}

		File mixinMapFile = mappingsProvider.MAPPINGS_MIXIN_EXPORT;
		Path mixinMapPath = mixinMapFile.toPath();

		TinyRemapper.Builder remapperBuilder = TinyRemapper.newRemapper();
		remapperBuilder = remapperBuilder.withMappings(TinyUtils.createTinyMappingProvider(mappings, fromM, toM));
		if (mixinMapFile.exists()) {
			remapperBuilder = remapperBuilder.withMappings(TinyUtils.createTinyMappingProvider(mixinMapPath, fromM, toM));
		}

		project.getLogger().lifecycle("Remapping " + modJar.getName());

		TinyRemapper remapper = remapperBuilder.build();

		try (OutputConsumerPath outputConsumer = new OutputConsumerPath(modJarOutputPath)) {
			outputConsumer.addNonClassFiles(modJarPath);
			remapper.readClassPath(classpath);
			remapper.readInputs(modJarPath);
			remapper.apply(outputConsumer);
			if (!skipATs && AccessTransformerHelper.obfATs(extension, task, remapper, outputConsumer)) {
				project.getLogger().info("Remapped access transformer");
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to remap JAR", e);
		} finally {
			remapper.finish();
		}

		if (!modJarOutput.exists()){
			throw new RuntimeException("Failed to reobfuscate JAR");
		}

		if (MixinRefmapHelper.addRefmapName(extension.getRefmapName(), extension.getMixinJsonVersion(), modJarOutput)) {
			project.getLogger().debug("Transformed mixin reference maps in output JAR!");
		}

		if (nest) {
			if (NestedJars.addNestedJars(project, modJarOutput)) {
				project.getLogger().debug("Added nested jar paths to mod json");
			}
		}

		try {
			if (modJar.exists()) {
				Files.move(modJar, modJarUnmappedCopy);
				extension.addUnmappedMod(modJarUnmappedCopy);
			}

			Files.move(modJarOutput, modJar);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
