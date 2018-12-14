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

package net.fabricmc.loom.task;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;

public class LoomFernflowerDecompiler extends ConsoleDecompiler {
	private final Map<String, int[]> differingMappings = new HashMap<>();
	private final String jarName;

	public LoomFernflowerDecompiler(File destination, String jarName, Map<String, Object> options, IFernflowerLogger logger) {
		super(destination, options, logger);
		this.jarName = jarName;
	}

	public void addSource(File source) {
		addSpace(source, true);
	}

	public void addLibrary(File library) {
		addSpace(library, false);
	}

	public Map<String, int[]> getDifferingMappings() {
		return differingMappings;
	}

	@Override
	public void saveFolder(String s) {
		super.saveFolder(s);
	}

	@Override
	public void copyFile(String s, String s1, String s2) {
		throw new RuntimeException("TODO copyFile " + s + " " + s1 + " " + s2);
	}

	@Override
	public void saveClassFile(String s, String s1, String s2, String s3, int[] ints) {
		throw new RuntimeException("TODO saveClassFile " + s + " " + s1 + " " + s2 + " " + s3);
	}

	@Override
	public void createArchive(String s, String s1, Manifest manifest) {
		super.createArchive(s, jarName, manifest);
	}

	@Override
	public void saveDirEntry(String s, String s1, String s2) {
		super.saveDirEntry(s, jarName, s2);
	}

	@Override
	public void copyEntry(String s, String s1, String s2, String s3) {
		if (s3.endsWith(".java")) {
			super.copyEntry(s, s1, jarName, s3);
		}
	}

	@Override
	public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
		if (qualifiedName != null && DecompilerContext.getOption(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING)) {
			int[] mapping = DecompilerContext.getBytecodeSourceMapper().getOriginalLinesMapping();
			differingMappings.put(qualifiedName, mapping);
		}

		super.saveClassEntry(path, jarName, qualifiedName, entryName, content);
	}

	@Override
	public void closeArchive(String s, String s1) {
		super.closeArchive(s, jarName);
	}
}
