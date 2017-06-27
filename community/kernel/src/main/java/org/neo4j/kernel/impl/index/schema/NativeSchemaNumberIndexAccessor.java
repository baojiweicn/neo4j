/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.index.schema;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.helpers.collection.BoundedIterable;
import org.neo4j.index.internal.gbptree.Layout;
import org.neo4j.index.internal.gbptree.RecoveryCleanupWorkCollector;
import org.neo4j.io.pagecache.IOLimiter;
import org.neo4j.io.pagecache.PageCache;
import org.neo4j.kernel.api.exceptions.index.IndexEntryConflictException;
import org.neo4j.kernel.api.index.IndexAccessor;
import org.neo4j.kernel.api.index.IndexUpdater;
import org.neo4j.kernel.api.index.PropertyAccessor;
import org.neo4j.kernel.impl.api.index.IndexUpdateMode;
import org.neo4j.kernel.impl.index.GBPTreeUtil;
import org.neo4j.storageengine.api.schema.IndexReader;

import static org.neo4j.helpers.collection.Iterators.asResourceIterator;
import static org.neo4j.helpers.collection.Iterators.iterator;

public class NativeSchemaNumberIndexAccessor<KEY extends NumberKey, VALUE extends NumberValue>
        extends NativeSchemaNumberIndex<KEY,VALUE> implements IndexAccessor
{
    private final NativeSchemaNumberIndexUpdater<KEY,VALUE> singleUpdater;

    NativeSchemaNumberIndexAccessor( PageCache pageCache, File storeFile,
            Layout<KEY,VALUE> layout, RecoveryCleanupWorkCollector recoveryCleanupWorkCollector ) throws IOException
    {
        super( pageCache, storeFile, layout );
        singleUpdater = new NativeSchemaNumberIndexUpdater<>( layout.newKey(), layout.newValue() );
        instantiateTree( recoveryCleanupWorkCollector );
    }

    @Override
    public void drop() throws IOException
    {
        closeTree();
        GBPTreeUtil.delete( pageCache, storeFile );
    }

    @Override
    public IndexUpdater newUpdater( IndexUpdateMode mode )
    {
        assertOpen();
        try
        {
            return singleUpdater.initialize( tree.writer(), true );
        }
        catch ( IOException e )
        {
            throw new UncheckedIOException( e );
        }
    }

    @Override
    public void force() throws IOException
    {
        // TODO add IOLimiter arg
        tree.checkpoint( IOLimiter.unlimited() );
    }

    @Override
    public void close() throws IOException
    {
        closeTree();
    }

    @Override
    public IndexReader newReader()
    {
        assertOpen();
        return new NativeSchemaNumberIndexReader<>( tree, layout );
    }

    @Override
    public BoundedIterable<Long> newAllEntriesReader()
    {
        return new NumberAllEntriesReader<>( tree, layout );
    }

    @Override
    public ResourceIterator<File> snapshotFiles() throws IOException
    {
        return asResourceIterator( iterator( storeFile ) );
    }

    @Override
    public void verifyDeferredConstraints( PropertyAccessor propertyAccessor )
            throws IndexEntryConflictException, IOException
    {
        throw new UnsupportedOperationException( "Implement me" );
    }
}