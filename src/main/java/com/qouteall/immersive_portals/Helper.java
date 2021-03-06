package com.qouteall.immersive_portals;

import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.block_manipulation.BlockManipulationClient;
import com.qouteall.immersive_portals.ducks.IERayTraceContext;
import com.qouteall.immersive_portals.ducks.IEWorldChunk;
import com.qouteall.immersive_portals.my_util.IntBox;
import com.qouteall.immersive_portals.portal.Portal;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Quaternion;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Helper {
    
    private static final Logger LOGGER = LogManager.getLogger("Portal");
    
    public static FloatBuffer getModelViewMatrix() {
        return getMatrix(GL11.GL_MODELVIEW_MATRIX);
    }
    
    public static FloatBuffer getProjectionMatrix() {
        return getMatrix(GL11.GL_PROJECTION_MATRIX);
    }
    
    public static FloatBuffer getTextureMatrix() {
        return getMatrix(GL11.GL_TEXTURE_MATRIX);
    }
    
    public static FloatBuffer getMatrix(int matrixId) {
        FloatBuffer temp = BufferUtils.createFloatBuffer(16);
        
        GL11.glGetFloatv(matrixId, temp);
        
        return temp;
    }
    
    //get the intersect point of a line and a plane
    //a line: p = lineCenter + t * lineDirection
    //get the t of the colliding point
    //normal and lineDirection have to be normalized
    public static double getCollidingT(
        Vec3d planeCenter,
        Vec3d planeNormal,
        Vec3d lineCenter,
        Vec3d lineDirection
    ) {
        return (planeCenter.subtract(lineCenter).dotProduct(planeNormal))
            /
            (lineDirection.dotProduct(planeNormal));
    }
    
    public static boolean isInFrontOfPlane(
        Vec3d pos,
        Vec3d planePos,
        Vec3d planeNormal
    ) {
        return pos.subtract(planePos).dotProduct(planeNormal) > 0;
    }
    
    public static Vec3d fallPointOntoPlane(
        Vec3d point,
        Vec3d planePos,
        Vec3d planeNormal
    ) {
        double t = getCollidingT(planePos, planeNormal, point, planeNormal);
        return point.add(planeNormal.scale(t));
    }
    
    public static Vec3i getUnitFromAxis(Direction.Axis axis) {
        return Direction.getFacingFromAxis(
            Direction.AxisDirection.POSITIVE,
            axis
        ).getDirectionVec();
    }
    
    public static int getCoordinate(Vec3i v, Direction.Axis axis) {
        return axis.getCoordinate(v.getX(), v.getY(), v.getZ());
    }
    
    public static double getCoordinate(Vec3d v, Direction.Axis axis) {
        return axis.getCoordinate(v.x, v.y, v.z);
    }
    
    public static int getCoordinate(Vec3i v, Direction direction) {
        return getCoordinate(v, direction.getAxis()) *
            (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1);
    }
    
    public static Vec3d putCoordinate(Vec3d v, Direction.Axis axis, double value) {
        if (axis == Direction.Axis.X) {
            return new Vec3d(value, v.y, v.z);
        }
        else if (axis == Direction.Axis.Y) {
            return new Vec3d(v.x, value, v.z);
        }
        else {
            return new Vec3d(v.x, v.y, value);
        }
    }
    
    public static <A, B> Tuple<B, A> swaped(Tuple<A, B> p) {
        return new Tuple<>(p.getB(), p.getA());
    }
    
    public static <T> T uniqueOfThree(T a, T b, T c) {
        if (a.equals(b)) {
            return c;
        }
        else if (b.equals(c)) {
            return a;
        }
        else {
            assert a.equals(c);
            return b;
        }
    }
    
    public static BlockPos max(BlockPos a, BlockPos b) {
        return new BlockPos(
            Math.max(a.getX(), b.getX()),
            Math.max(a.getY(), b.getY()),
            Math.max(a.getZ(), b.getZ())
        );
    }
    
    public static BlockPos min(BlockPos a, BlockPos b) {
        return new BlockPos(
            Math.min(a.getX(), b.getX()),
            Math.min(a.getY(), b.getY()),
            Math.min(a.getZ(), b.getZ())
        );
    }
    
    public static Tuple<Direction.Axis, Direction.Axis> getAnotherTwoAxis(Direction.Axis axis) {
        switch (axis) {
            case X:
                return new Tuple<>(Direction.Axis.Y, Direction.Axis.Z);
            case Y:
                return new Tuple<>(Direction.Axis.Z, Direction.Axis.X);
            case Z:
                return new Tuple<>(Direction.Axis.X, Direction.Axis.Y);
        }
        throw new IllegalArgumentException();
    }
    
    public static BlockPos scale(Vec3i v, int m) {
        return new BlockPos(v.getX() * m, v.getY() * m, v.getZ() * m);
    }
    
    public static BlockPos divide(Vec3i v, int d) {
        return new BlockPos(v.getX() / d, v.getY() / d, v.getZ() / d);
    }
    
    public static Direction[] getAnotherFourDirections(Direction.Axis axisOfNormal) {
        Tuple<Direction.Axis, Direction.Axis> anotherTwoAxis = getAnotherTwoAxis(
            axisOfNormal
        );
        return new Direction[]{
            Direction.getFacingFromAxis(
                Direction.AxisDirection.POSITIVE, anotherTwoAxis.getA()
            ),
            Direction.getFacingFromAxis(
                Direction.AxisDirection.POSITIVE, anotherTwoAxis.getB()
            ),
            Direction.getFacingFromAxis(
                Direction.AxisDirection.NEGATIVE, anotherTwoAxis.getA()
            ),
            Direction.getFacingFromAxis(
                Direction.AxisDirection.NEGATIVE, anotherTwoAxis.getB()
            )
        };
    }
    
    @Deprecated
    public static Tuple<Direction.Axis, Direction.Axis> getPerpendicularAxis(Direction facing) {
        Tuple<Direction.Axis, Direction.Axis> axises = getAnotherTwoAxis(facing.getAxis());
        if (facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            axises = new Tuple<>(axises.getB(), axises.getA());
        }
        return axises;
    }
    
    public static Tuple<Direction, Direction> getPerpendicularDirections(Direction facing) {
        Tuple<Direction.Axis, Direction.Axis> axises = getAnotherTwoAxis(facing.getAxis());
        if (facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            axises = new Tuple<>(axises.getB(), axises.getA());
        }
        return new Tuple<>(
            Direction.getFacingFromAxis(Direction.AxisDirection.POSITIVE, axises.getA()),
            Direction.getFacingFromAxis(Direction.AxisDirection.POSITIVE, axises.getB())
        );
    }
    
    public static Vec3d getBoxSize(AxisAlignedBB box) {
        return new Vec3d(box.getXSize(), box.getYSize(), box.getZSize());
    }
    
    public static AxisAlignedBB getBoxSurface(AxisAlignedBB box, Direction direction) {
        double size = getCoordinate(getBoxSize(box), direction.getAxis());
        Vec3d shrinkVec = new Vec3d(direction.getDirectionVec()).scale(size);
        return box.contract(shrinkVec.x, shrinkVec.y, shrinkVec.z);
    }
    
    public static IntBox expandRectangle(
        BlockPos startingPos,
        Predicate<BlockPos> blockPosPredicate, Direction.Axis axis
    ) {
        IntBox wallArea = new IntBox(startingPos, startingPos);
        
        for (Direction direction : getAnotherFourDirections(axis)) {
            
            wallArea = expandArea(
                wallArea,
                blockPosPredicate,
                direction
            );
        }
        return wallArea;
    }
    
    public static int getChebyshevDistance(
        int x1, int z1,
        int x2, int z2
    ) {
        return Math.max(
            Math.abs(x1 - x2),
            Math.abs(z1 - z2)
        );
    }
    
    public static class SimpleBox<T> {
        public T obj;
        
        public SimpleBox(T obj) {
            this.obj = obj;
        }
    }
    
    //@Nullable
    public static <T> T getLastSatisfying(Stream<T> stream, Predicate<T> predicate) {
        SimpleBox<T> box = new SimpleBox<T>(null);
        stream.filter(curr -> {
            if (predicate.test(curr)) {
                box.obj = curr;
                return false;
            }
            else {
                return true;
            }
        }).findFirst();
        return box.obj;
    }
    
    public interface CallableWithoutException<T> {
        public T run();
    }
    
    public static Vec3d interpolatePos(Entity entity, float partialTicks) {
        Vec3d currPos = entity.getPositionVec();
        Vec3d lastTickPos = McHelper.lastTickPosOf(entity);
        return lastTickPos.add(currPos.subtract(lastTickPos).scale(partialTicks));
    }
    
    public static Runnable noException(Callable func) {
        return () -> {
            try {
                func.call();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        };
    }
    
    public static void doNotEatExceptionMessage(
        Runnable func
    ) {
        try {
            func.run();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    public static <T> String myToString(
        Stream<T> stream
    ) {
        StringBuilder stringBuilder = new StringBuilder();
        stream.forEach(obj -> {
            stringBuilder.append(obj.toString());
            stringBuilder.append('\n');
        });
        return stringBuilder.toString();
    }
    
    public static <A, B> Stream<Tuple<A, B>> composeTwoStreamsWithEqualLength(
        Stream<A> a,
        Stream<B> b
    ) {
        Iterator<A> aIterator = a.iterator();
        Iterator<B> bIterator = b.iterator();
        Iterator<Tuple<A, B>> iterator = new Iterator<Tuple<A, B>>() {
            
            @Override
            public boolean hasNext() {
                assert aIterator.hasNext() == bIterator.hasNext();
                return aIterator.hasNext();
            }
            
            @Override
            public Tuple<A, B> next() {
                return new Tuple<>(aIterator.next(), bIterator.next());
            }
        };
        
        return Streams.stream(iterator);
    }
    
    public static void log(Object str) {
        LOGGER.info(str);
    }
    
    public static void err(Object str) {
        LOGGER.error(str);
    }
    
    public static void dbg(Object str) {
        LOGGER.debug(str);
    }
    
    public static Vec3d[] eightVerticesOf(AxisAlignedBB box) {
        return new Vec3d[]{
            new Vec3d(box.minX, box.minY, box.minZ),
            new Vec3d(box.minX, box.minY, box.maxZ),
            new Vec3d(box.minX, box.maxY, box.minZ),
            new Vec3d(box.minX, box.maxY, box.maxZ),
            new Vec3d(box.maxX, box.minY, box.minZ),
            new Vec3d(box.maxX, box.minY, box.maxZ),
            new Vec3d(box.maxX, box.maxY, box.minZ),
            new Vec3d(box.maxX, box.maxY, box.maxZ)
        };
    }
    
    public static void putVec3d(CompoundNBT compoundTag, String name, Vec3d vec3d) {
        compoundTag.putDouble(name + "X", vec3d.x);
        compoundTag.putDouble(name + "Y", vec3d.y);
        compoundTag.putDouble(name + "Z", vec3d.z);
    }
    
    public static Vec3d getVec3d(CompoundNBT compoundTag, String name) {
        return new Vec3d(
            compoundTag.getDouble(name + "X"),
            compoundTag.getDouble(name + "Y"),
            compoundTag.getDouble(name + "Z")
        );
    }
    
    public static void putVec3i(CompoundNBT compoundTag, String name, Vec3i vec3i) {
        compoundTag.putInt(name + "X", vec3i.getX());
        compoundTag.putInt(name + "Y", vec3i.getY());
        compoundTag.putInt(name + "Z", vec3i.getZ());
    }
    
    public static BlockPos getVec3i(CompoundNBT compoundTag, String name) {
        return new BlockPos(
            compoundTag.getInt(name + "X"),
            compoundTag.getInt(name + "Y"),
            compoundTag.getInt(name + "Z")
        );
    }
    
    public static <T> void compareOldAndNew(
        Set<T> oldSet,
        Set<T> newSet,
        Consumer<T> forRemoved,
        Consumer<T> forAdded
    ) {
        oldSet.stream().filter(
            e -> !newSet.contains(e)
        ).forEach(
            forRemoved
        );
        newSet.stream().filter(
            e -> !oldSet.contains(e)
        ).forEach(
            forAdded
        );
    }
    
    public static long secondToNano(double second) {
        return (long) (second * 1000000000L);
    }
    
    public static double nanoToSecond(long nano) {
        return nano / 1000000000.0;
    }
    
    public static IntBox expandArea(
        IntBox originalArea,
        Predicate<BlockPos> predicate,
        Direction direction
    ) {
        IntBox currentBox = originalArea;
        for (int i = 1; i < 42; i++) {
            IntBox expanded = currentBox.getExpanded(direction, 1);
            if (expanded.getSurfaceLayer(direction).stream().allMatch(predicate)) {
                currentBox = expanded;
            }
            else {
                return currentBox;
            }
        }
        return currentBox;
    }
    
    public static <A, B> B reduce(
        B start,
        Stream<A> stream,
        BiFunction<B, A, B> func
    ) {
        return stream.reduce(
            start,
            func,
            (a, b) -> {
                throw new IllegalStateException("combiner should only be used in parallel");
            }
        );
    }
    
    public static <T> T noError(Callable<T> func) {
        try {
            return func.call();
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
    
    public static interface ExceptionalRunnable {
        void run() throws Throwable;
    }
    
    public static void noError(ExceptionalRunnable runnable) {
        try {
            runnable.run();
        }
        catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
    
    //ObjectList does not override removeIf() so its complexity is O(n^2)
    //this is O(n)
    public static <T> void removeIf(ObjectList<T> list, Predicate<T> predicate) {
        int placingIndex = 0;
        for (int i = 0; i < list.size(); i++) {
            T curr = list.get(i);
            if (!predicate.test(curr)) {
                list.set(placingIndex, curr);
                placingIndex += 1;
            }
        }
        list.removeElements(placingIndex, list.size());
    }
    
    public static <T, S> Stream<S> wrapAdjacentAndMap(
        Stream<T> stream,
        BiFunction<T, T, S> function
    ) {
        Iterator<T> iterator = stream.iterator();
        return Streams.stream(new Iterator<S>() {
            private boolean isBuffered = false;
            private T buffer;
            
            private void fillBuffer() {
                if (!isBuffered) {
                    assert iterator.hasNext();
                    isBuffered = true;
                    buffer = iterator.next();
                }
            }
            
            private T takeBuffer() {
                assert isBuffered;
                isBuffered = false;
                return buffer;
            }
            
            @Override
            public boolean hasNext() {
                if (!iterator.hasNext()) {
                    return false;
                }
                fillBuffer();
                return iterator.hasNext();
            }
            
            @Override
            public S next() {
                fillBuffer();
                T a = takeBuffer();
                fillBuffer();
                return function.apply(a, buffer);
            }
        });
    }
    
    //map and reduce at the same time
    public static <A, B> Stream<B> mapReduce(
        Stream<A> stream,
        BiFunction<B, A, B> func,
        SimpleBox<B> startValue
    ) {
        return stream.map(a -> {
            startValue.obj = func.apply(startValue.obj, a);
            return startValue.obj;
        });
    }
    
    //another implementation using mapReduce but creates more garbage objects
    public static <T, S> Stream<S> wrapAdjacentAndMap1(
        Stream<T> stream,
        BiFunction<T, T, S> function
    ) {
        Iterator<T> iterator = stream.iterator();
        if (!iterator.hasNext()) {
            return Stream.empty();
        }
        T firstValue = iterator.next();
        Stream<T> newStream = Streams.stream(iterator);
        return mapReduce(
            newStream,
            (Tuple<T, S> lastPair, T curr) ->
                new Tuple<T, S>(curr, function.apply(lastPair.getA(), curr)),
            new SimpleBox<>(new Tuple<T, S>(firstValue, null))
        ).map(pair -> pair.getB());
    }
    
    public static <T> T makeIntoExpression(T t, Consumer<T> func) {
        func.accept(t);
        return t;
    }
    
    //NOTE this will mutate a and return a
    public static Quaternion quaternionNumAdd(Quaternion a, Quaternion b) {
        //TODO correct wrong parameter name for yarn
        a.set(
            a.getX() + b.getX(),
            a.getY() + b.getY(),
            a.getZ() + b.getZ(),
            a.getW() + b.getW()
        );
        return a;
    }
    
    //NOTE this will mutate a and reutrn a
    public static Quaternion quaternionScale(Quaternion a, float scale) {
        a.set(
            a.getX() * scale,
            a.getY() * scale,
            a.getZ() * scale,
            a.getW() * scale
        );
        return a;
    }
    
    //a quaternion is a 4d vector on 4d sphere
    //this method may mutate argument but will not change rotation
    public static Quaternion interpolateQuaternion(
        Quaternion a,
        Quaternion b,
        float t
    ) {
        a.normalize();
        b.normalize();
        
        double dot = dotProduct4d(a, b);
        
        if (dot < 0.0f) {
            a.multiply(-1);
            dot = -dot;
        }
        
        double DOT_THRESHOLD = 0.9995;
        if (dot > DOT_THRESHOLD) {
            // If the inputs are too close for comfort, linearly interpolate
            // and normalize the result.
            
            Quaternion result = quaternionNumAdd(
                quaternionScale(a.copy(), 1 - t),
                quaternionScale(b.copy(), t)
            );
            result.normalize();
            return result;
        }
        
        double theta_0 = Math.acos(dot);
        double theta = theta_0 * t;
        double sin_theta = Math.sin(theta);
        double sin_theta_0 = Math.sin(theta_0);
        
        double s0 = Math.cos(theta) - dot * sin_theta / sin_theta_0;
        double s1 = sin_theta / sin_theta_0;
        
        return quaternionNumAdd(
            quaternionScale(a.copy(), (float) s0),
            quaternionScale(b.copy(), (float) s1)
        );
    }
    
    public static double dotProduct4d(Quaternion a, Quaternion b) {
        return a.getW() * b.getW() +
            a.getX() * b.getX() +
            a.getY() * b.getY() +
            a.getZ() * b.getZ();
    }
    
    public static boolean isClose(Quaternion a, Quaternion b, float valve) {
        a.normalize();
        b.normalize();
        if (a.getW() * b.getW() < 0) {
            a.multiply(-1);
        }
        float da = a.getW() - b.getW();
        float db = a.getX() - b.getX();
        float dc = a.getY() - b.getY();
        float dd = a.getZ() - b.getZ();
        return da * da + db * db + dc * dc + dd * dd < valve;
    }
    
    public static Vec3d getRotated(Quaternion rotation, Vec3d vec) {
        Vector3f vector3f = new Vector3f(vec);
        vector3f.transform(rotation);
        return new Vec3d(vector3f);
    }
    
    public static Quaternion ortholize(Quaternion quaternion) {
        if (quaternion.getW() < 0) {
            quaternion.multiply(-1);
        }
        return quaternion;
    }
    
    //naive interpolation is better?
    //not better
    public static Quaternion interpolateQuaternionNaive(
        Quaternion a,
        Quaternion b,
        float t
    ) {
        return makeIntoExpression(
            new Quaternion(
                MathHelper.lerp(t, a.getX(), b.getX()),
                MathHelper.lerp(t, a.getY(), b.getY()),
                MathHelper.lerp(t, a.getZ(), b.getZ()),
                MathHelper.lerp(t, a.getW(), b.getW())
            ),
            Quaternion::normalize
        );
    }
    
    /**
     * Searches nearby chunks to look for a certain sub/class of entity. In the specified {@code world}, the chunk that
     * {@code pos} is in will be used as the center of search. That chunk will be expanded by {@code chunkRadius} chunks
     * in all directions to define the search area. Then, on all Y levels, those chunks will be searched for entities of
     * class {@code entityClass}. Then all entities found will be returned.
     * <p>
     * If you define a {@code chunkRadius} of 1, 9 chunks will be searched. If you define one of 2, then 25 chunks will
     * be searched. This can be an extreme performance bottleneck, so yse it sparingly such as a response to user input.
     *
     * @param world       The world in which to search for entities.
     * @param pos         The chunk that this position is located in will be used as the center of search.
     * @param chunkRadius Integer number of chunks to expand the square search area by.
     * @param entityClass The entity class to search for.
     * @param <T>         The entity class that will be returned in the list.
     * @return All entities in the nearby chunks with type T.
     * @author LoganDark
     */
    @SuppressWarnings("WeakerAccess")
    public static <T extends Entity> List<T> getNearbyEntities(
        World world,
        Vec3d pos,
        int chunkRadius,
        Class<T> entityClass
    ) {
        ArrayList<T> entities = new ArrayList<>();
        int chunkX = (int) pos.x / 16;
        int chunkZ = (int) pos.z / 16;
        
        for (int z = -chunkRadius + 1; z < chunkRadius; z++) {
            for (int x = -chunkRadius + 1; x < chunkRadius; x++) {
                int aX = chunkX + x;
                int aZ = chunkZ + z;
                
                // WorldChunk contains a private variable called entitySections that groups all entities in the chunk by
                // their Y level. Here we are using a Mixin duck typing interface thing to get that private variable and
                // then manually search it. This is faster than using the built-in WorldChunk methods that do not do
                // what we want.
                ClassInheritanceMultiMap<Entity>[] entitySections = ((IEWorldChunk) world.getChunk(
                    aX,
                    aZ
                )).getEntitySections();
                for (ClassInheritanceMultiMap<Entity> entitySection : entitySections) {
                    entities.addAll(entitySection.getByClass(entityClass));
                }
            }
        }
        
        return entities;
    }
    
    /**
     * Returns all portals intersecting the line from start->end.
     *
     * @param world                The world in which to ray trace for portals.
     * @param start                The start of the line defining the ray to trace.
     * @param end                  The end of the line defining the ray to trace.
     * @param includeGlobalPortals Whether or not to include global portals in the ray trace.
     * @param filter               Filter the portals that this function returns. Nullable
     * @return A list of portals and their intersection points with the line, sorted by nearest portals first.
     * @author LoganDark
     */
    @SuppressWarnings("WeakerAccess")
    public static List<Tuple<Portal, Vec3d>> rayTracePortals(
        World world,
        Vec3d start,
        Vec3d end,
        boolean includeGlobalPortals,
        Predicate<Portal> filter
    ) {
        // This will be the center of the chunk search, rather than using start or end. This will allow the radius to be
        // smaller, and as a result, the search to be faster and slightly less inefficient.
        //
        // The searching method employed by getNearbyEntities is still not ideal, but it's the best idea I have.
        Vec3d middle = start.scale(0.5).add(end.scale(0.5));
        
        // This could result in searching more chunks than necessary, but it always expands to completely cover any
        // chunks the line from start->end passes through.
        int chunkRadius = (int) Math.ceil(Math.abs(start.distanceTo(end) / 2) / 16);
        List<Portal> nearby = getNearbyEntities(world, middle, chunkRadius, Portal.class);
        
        if (includeGlobalPortals) {
            nearby.addAll(McHelper.getGlobalPortals(world));
        }
        
        // Make a list of all portals actually intersecting with this line, and then sort them by the distance from the
        // start position. Nearest portals first.
        List<Tuple<Portal, Vec3d>> hits = new ArrayList<>();
        
        nearby.forEach(portal -> {
            if (filter == null || filter.test(portal)) {
                Vec3d intersection = portal.pick(start, end);
                
                if (intersection != null) {
                    hits.add(new Tuple<>(portal, intersection));
                }
            }
        });
        
        hits.sort((pair1, pair2) -> {
            Vec3d intersection1 = pair1.getB();
            Vec3d intersection2 = pair2.getB();
            
            // Return a negative number if intersection1 is smaller (should come first)
            return (int) Math.signum(intersection1.squareDistanceTo(start) - intersection2.squareDistanceTo(
                start));
        });
        
        return hits;
    }
    
    /**
     * @see #withSwitchedContext(World, Supplier)
     */
    @OnlyIn(Dist.CLIENT)
    private static <T> T withSwitchedContextClient(ClientWorld world, Supplier<T> func) {
        boolean wasContextSwitched = BlockManipulationClient.isContextSwitched;
        Minecraft mc = Minecraft.getInstance();
        ClientWorld lastWorld = mc.world;
        
        try {
            BlockManipulationClient.isContextSwitched = true;
            mc.world = world;
            
            return func.get();
        }
        finally {
            mc.world = lastWorld;
            BlockManipulationClient.isContextSwitched = wasContextSwitched;
        }
    }
    
    /**
     * @see #withSwitchedContext(World, Supplier)
     */
    @SuppressWarnings("unused")
    private static <T> T withSwitchedContextServer(ServerWorld world, Supplier<T> func) {
        // lol
        return func.get();
    }
    
    /**
     * Execute {@code func} with the world being set to {@code world}, hopefully bypassing any issues that may be
     * related to mutating a world that is not currently set as the current world.
     * <p>
     * You may safely nest this function within other context switches. It works on both the client and the server.
     *
     * @param world The world to switch the context to. The context will be restored when {@code func} is complete.
     * @param func  The function to execute while the context is switched.
     * @param <T>   The return type of {@code func}.
     * @return Whatever {@code func} returned.
     */
    private static <T> T withSwitchedContext(World world, Supplier<T> func) {
        if (world.isRemote) {
            return withSwitchedContextClient((ClientWorld) world, func);
        }
        else {
            return withSwitchedContextServer((ServerWorld) world, func);
        }
    }
    
    /**
     * @author LoganDark
     * @see Helper#rayTrace(World, RayTraceContext, boolean)
     */
    private static Tuple<BlockRayTraceResult, List<Portal>> rayTrace(
        World world,
        RayTraceContext context,
        boolean includeGlobalPortals,
        List<Portal> portals
    ) {
        Vec3d start = context.func_222253_b();
        Vec3d end = context.func_222250_a();
        
        // If we're past the max portal layer, don't let the player target behind this portal, create a missed result
        if (portals.size() > Global.maxPortalLayer) {
            Vec3d diff = end.subtract(start);
            
            return new Tuple<>(
                BlockRayTraceResult.createMiss(
                    end,
                    Direction.getFacingFromVector(diff.x, diff.y, diff.z),
                    new BlockPos(end)
                ),
                portals
            );
        }
        
        // First ray trace normally
        BlockRayTraceResult hitResult = world.rayTraceBlocks(context);
        
        List<Tuple<Portal, Vec3d>> rayTracedPortals = withSwitchedContext(
            world,
            () -> rayTracePortals(world, start, end, includeGlobalPortals, Portal::isInteractable)
        );
        
        if (rayTracedPortals.isEmpty()) {
            return new Tuple<>(hitResult, portals);
        }
        
        Tuple<Portal, Vec3d> portalHit = rayTracedPortals.get(0);
        Portal portal = portalHit.getA();
        Vec3d intersection = portalHit.getB();
        
        // If the portal is not closer, return the hit result we just got
        if (hitResult.getHitVec().squareDistanceTo(start) < intersection.squareDistanceTo(start)) {
            return new Tuple<>(hitResult, portals);
        }
        
        // If the portal is closer, recurse
        
        IERayTraceContext betterContext = (IERayTraceContext) context;
        
        betterContext
            .setStart(portal.transformPoint(intersection))
            .setEnd(portal.transformPoint(end));
        
        portals.add(portal);
        World destWorld = portal.getDestinationWorld(world.isRemote);
        Tuple<BlockRayTraceResult, List<Portal>> recursion = withSwitchedContext(
            destWorld,
            () -> rayTrace(destWorld, context, includeGlobalPortals, portals)
        );
        
        betterContext
            .setStart(start)
            .setEnd(end);
        
        return recursion;
    }
    
    /**
     * Ray traces for blocks or whatever the {@code context} dictates.
     *
     * @param world                The world to ray trace in.
     * @param context              The ray tracing context to use. This context will be mutated as it goes but will be
     *                             returned back to normal before a result is returned to you, so you can act like it
     *                             hasn't been  mutated.
     * @param includeGlobalPortals Whether or not to include global portals in the ray trace. If this is false, then the
     *                             ray trace can pass right through them.
     * @return The BlockHitResult and the list of portals that we've passed through to get there. This list can be used
     * to transform looking directions or do whatever you want really.
     * @author LoganDark
     */
    @SuppressWarnings("WeakerAccess")
    public static Tuple<BlockRayTraceResult, List<Portal>> rayTrace(
        World world,
        RayTraceContext context,
        boolean includeGlobalPortals
    ) {
        return rayTrace(world, context, includeGlobalPortals, new ArrayList<>());
    }
    
    /**
     * @param hitResult The HitResult to check.
     * @return If the HitResult passed is either {@code null}, or of type {@link HitResult.Type#MISS}.
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean hitResultIsMissedOrNull(RayTraceResult hitResult) {
        return hitResult == null || hitResult.getType() == RayTraceResult.Type.MISS;
    }
    
    /**
     * @param vec  The {@link Vec3d} to get the {@link Direction} of.
     * @param axis The {@link Direction.Axis} of directions to exclude.
     * @return The {@link Direction} of the passed {@code vec}, excluding directions of axis {@code axis}.
     */
    @SuppressWarnings("WeakerAccess")
    public static Direction getFacingExcludingAxis(Vec3d vec, Direction.Axis axis) {
        return Arrays.stream(Direction.values())
            .filter(d -> d.getAxis() != axis)
            .max(Comparator.comparingDouble(
                dir -> vec.x * dir.getXOffset() + vec.y * dir.getYOffset() + vec.z * dir.getZOffset()
            ))
            .orElse(null);
    }
    
    /**
     * Places a portal based on {@code entity}'s looking direction. Does not set the portal destination or add it to the
     * world, you will have to do that yourself.
     *
     * @param width  The width of the portal.
     * @param height The height of the portal.
     * @param entity The entity to place this portal as.
     * @return The placed portal, with no destination set.
     * @author LoganDark
     */
    public static Portal placePortal(double width, double height, Entity entity) {
        Vec3d playerLook = entity.getLookVec();
        
        Tuple<BlockRayTraceResult, List<Portal>> rayTrace =
            rayTrace(
                entity.world,
                new RayTraceContext(
                    entity.getEyePosition(1.0f),
                    entity.getEyePosition(1.0f).add(playerLook.scale(100.0)),
                    RayTraceContext.BlockMode.OUTLINE,
                    RayTraceContext.FluidMode.NONE,
                    entity
                ),
                true
            );
        
        BlockRayTraceResult hitResult = rayTrace.getA();
        List<Portal> hitPortals = rayTrace.getB();
        
        if (hitResultIsMissedOrNull(hitResult)) {
            return null;
        }
        
        for (Portal hitPortal : hitPortals) {
            playerLook = hitPortal.transformLocalVec(playerLook);
        }
        
        Direction lookingDirection = getFacingExcludingAxis(
            playerLook,
            hitResult.getFace().getAxis()
        );
        
        // this should never happen...
        if (lookingDirection == null) {
            return null;
        }
        
        Vec3d axisH = new Vec3d(hitResult.getFace().getDirectionVec());
        Vec3d axisW = axisH.crossProduct(new Vec3d(lookingDirection.getOpposite().getDirectionVec()));
        Vec3d pos = new Vec3d(hitResult.getPos()).add(.5, .5, .5)
            .add(axisH.scale(0.5 + height / 2));
        
        World world = hitPortals.isEmpty()
            ? entity.world
            : hitPortals.get(hitPortals.size() - 1).getDestinationWorld(false);
        
        Portal portal = new Portal(Portal.entityType, world);
        
        portal.setRawPosition(pos.x, pos.y, pos.z);
        
        portal.axisW = axisW;
        portal.axisH = axisH;
        
        portal.width = width;
        portal.height = height;
        
        return portal;
    }
}
