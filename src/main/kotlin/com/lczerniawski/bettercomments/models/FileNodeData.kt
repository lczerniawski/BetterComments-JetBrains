package com.lczerniawski.bettercomments.models

import com.intellij.openapi.vfs.VirtualFile

data class FileNodeData(val file: VirtualFile, val comments: List<CommentNodeData>)
