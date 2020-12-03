package com.github.kristianvld.angeltrophies.util;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BlockVector;

import java.nio.ByteBuffer;

public class BlockVectorTagType implements PersistentDataType<byte[], BlockVector> {

    public final static BlockVectorTagType BLOCK_VECTOR = new BlockVectorTagType();

    @Override
    public Class<byte[]> getPrimitiveType() {
        return byte[].class;
    }

    @Override
    public Class<BlockVector> getComplexType() {
        return BlockVector.class;
    }

    @Override
    public byte[] toPrimitive(BlockVector complex, PersistentDataAdapterContext context) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[12]);
        bb.putInt(complex.getBlockX());
        bb.putInt(complex.getBlockY());
        bb.putInt(complex.getBlockZ());
        return bb.array();
    }

    @Override
    public BlockVector fromPrimitive(byte[] primitive, PersistentDataAdapterContext context) {
        ByteBuffer bb = ByteBuffer.wrap(primitive);
        int x = bb.getInt();
        int y = bb.getInt();
        int z = bb.getInt();
        return new BlockVector(x, y, z);
    }
}
