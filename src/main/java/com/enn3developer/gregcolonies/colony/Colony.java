package com.enn3developer.gregcolonies.colony;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.enn3developer.gregcolonies.Config;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommand;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommandRegistry;

public class Colony {

    public static final int MAX_BLUEPRINTS = 16;

    public static final int MAX_HOMES = 32;

    private static final int BUILD_LEASE_TICKS = 100;

    private int id;
    private String name;
    private UUID owner;
    private String ownerName;
    private int dimension;
    private int x;
    private int y;
    private int z;
    private final Map<ColonySiteKind, ColonySite> sites = new EnumMap<>(ColonySiteKind.class);
    private final List<Blueprint> blueprints = new ArrayList<>();
    private int activeBlueprint = -1;
    private int placeRotation;
    private boolean placeMirror;
    private BuildSite buildSite;
    private UUID buildOwner;
    private long buildLease;
    private final List<ColonyHome> homes = new ArrayList<>();
    private int nextHomeId = 1;
    private final Deque<CitizenCommand> orders = new ArrayDeque<>();
    private final Map<UUID, ColonyCitizen> citizens = new LinkedHashMap<>();

    private Colony() {
        for (ColonySiteKind kind : ColonySiteKind.values()) {
            sites.put(kind, new ColonySite());
        }
    }

    public Colony(int id, String name, UUID owner, String ownerName, int dimension, int x, int y, int z) {
        this();
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.ownerName = ownerName;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public int getDimension() {
        return dimension;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public ColonySite site(ColonySiteKind kind) {
        return sites.get(kind);
    }

    public List<Blueprint> getBlueprints() {
        return blueprints;
    }

    public Blueprint getBlueprint(int index) {
        return index >= 0 && index < blueprints.size() ? blueprints.get(index) : null;
    }

    public int getActiveBlueprintIndex() {
        return activeBlueprint >= 0 && activeBlueprint < blueprints.size() ? activeBlueprint : -1;
    }

    public Blueprint getActiveBlueprint() {
        return getBlueprint(getActiveBlueprintIndex());
    }

    public int addBlueprint(Blueprint blueprint) {
        if (blueprint == null || blueprints.size() >= MAX_BLUEPRINTS) {
            return -1;
        }
        blueprints.add(blueprint);
        activeBlueprint = blueprints.size() - 1;
        return activeBlueprint;
    }

    public boolean replaceBlueprint(int index, Blueprint blueprint) {
        if (blueprint == null || index < 0 || index >= blueprints.size()) {
            return false;
        }
        blueprints.set(index, blueprint);
        activeBlueprint = index;
        return true;
    }

    public boolean removeBlueprint(int index) {
        if (index < 0 || index >= blueprints.size()) {
            return false;
        }
        blueprints.remove(index);
        if (activeBlueprint > index) {
            activeBlueprint--;
        } else if (activeBlueprint == index) {
            activeBlueprint = blueprints.isEmpty() ? -1 : Math.min(index, blueprints.size() - 1);
        }
        return true;
    }

    public boolean renameBlueprint(int index, String name) {
        Blueprint blueprint = getBlueprint(index);
        if (blueprint == null) {
            return false;
        }
        blueprint.setName(name);
        return true;
    }

    public boolean setActiveBlueprint(int index) {
        if (index < 0 || index >= blueprints.size()) {
            return false;
        }
        activeBlueprint = index;
        return true;
    }

    public int getPlaceRotation() {
        return placeRotation;
    }

    public void setPlaceRotation(int rotation) {
        placeRotation = ((rotation % Blueprint.ROTATIONS) + Blueprint.ROTATIONS) % Blueprint.ROTATIONS;
    }

    public boolean isPlaceMirror() {
        return placeMirror;
    }

    public void setPlaceMirror(boolean mirror) {
        placeMirror = mirror;
    }

    public BuildSite getBuildSite() {
        return buildSite;
    }

    public void setBuildSite(BuildSite buildSite) {
        this.buildSite = buildSite;
        this.buildOwner = null;
    }

    public boolean claimBuildSite(UUID id, long time) {
        if (buildOwner != null && !buildOwner.equals(id) && time - buildLease < BUILD_LEASE_TICKS) {
            return false;
        }
        buildOwner = id;
        buildLease = time;
        return true;
    }

    public void releaseBuildSite(UUID id) {
        if (buildOwner != null && buildOwner.equals(id)) {
            buildOwner = null;
        }
    }

    public List<ColonyHome> getHomes() {
        return homes;
    }

    public ColonyHome getHome(int id) {
        for (ColonyHome home : homes) {
            if (home.getId() == id) {
                return home;
            }
        }
        return null;
    }

    public ColonyHome getHomeAt(int x, int y, int z) {
        for (ColonyHome home : homes) {
            if (home.contains(x, y, z)) {
                return home;
            }
        }
        return null;
    }

    public boolean overlapsHome(WorkArea area) {
        for (ColonyHome home : homes) {
            if (home.getArea()
                .overlaps(area)) {
                return true;
            }
        }
        return false;
    }

    public ColonyHome addHome(WorkArea area, int beds) {
        if (homes.size() >= MAX_HOMES || overlapsHome(area)) {
            return null;
        }
        ColonyHome home = new ColonyHome(nextHomeId++, area, beds);
        homes.add(home);
        return home;
    }

    public boolean removeHome(int id) {
        ColonyHome home = getHome(id);
        if (home == null) {
            return false;
        }
        homes.remove(home);
        for (ColonyCitizen citizen : citizens.values()) {
            if (citizen.getHomeId() == id) {
                citizen.clearHome();
                citizen.clearBed();
            }
        }
        return true;
    }

    public int homeOccupants(int id) {
        int occupants = 0;
        for (ColonyCitizen citizen : citizens.values()) {
            if (citizen.getHomeId() == id) {
                occupants++;
            }
        }
        return occupants;
    }

    public boolean claimHome(UUID id, int homeId) {
        ColonyCitizen owner = citizens.get(id);
        ColonyHome home = getHome(homeId);
        if (owner == null || home == null) {
            return false;
        }
        if (owner.getHomeId() == homeId) {
            return true;
        }
        if (homeOccupants(homeId) >= home.getBeds()) {
            return false;
        }
        owner.setHomeId(homeId);
        return true;
    }

    public void releaseHome(UUID id) {
        ColonyCitizen owner = citizens.get(id);
        if (owner != null) {
            owner.clearHome();
        }
    }

    public boolean isBedFree(UUID id, int x, int y, int z) {
        for (ColonyCitizen citizen : citizens.values()) {
            if (!citizen.getId()
                .equals(id) && citizen.isBedAt(x, y, z)) {
                return false;
            }
        }
        return true;
    }

    public boolean claimBed(UUID id, int x, int y, int z) {
        ColonyCitizen owner = citizens.get(id);
        if (owner == null || !isBedFree(id, x, y, z) || !ownsBedAt(owner, x, y, z)) {
            return false;
        }
        owner.setBed(x, y, z);
        return true;
    }

    public boolean ownsBedAt(ColonyCitizen owner, int x, int y, int z) {
        ColonyHome home = getHomeAt(x, y, z);
        return home == null ? !owner.hasHome() : home.getId() == owner.getHomeId();
    }

    public void releaseBed(UUID id) {
        ColonyCitizen owner = citizens.get(id);
        if (owner != null) {
            owner.clearBed();
        }
    }

    public ColonyCitizen registerCitizen(EntityCitizen citizen) {
        ColonyCitizen entry = citizens.get(citizen.getUniqueID());
        if (entry == null) {
            entry = new ColonyCitizen(citizen);
            citizens.put(entry.getId(), entry);
        } else {
            entry.setName(citizen.getCitizenName());
            entry.updateState(citizen);
            entry.updatePosition(citizen);
        }
        return entry;
    }

    public boolean hasCitizenNamed(String name) {
        for (ColonyCitizen citizen : citizens.values()) {
            if (citizen.getName()
                .equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public ColonyCitizen getCitizen(UUID id) {
        return citizens.get(id);
    }

    public boolean removeCitizen(UUID id) {
        return citizens.remove(id) != null;
    }

    public Collection<ColonyCitizen> getCitizens() {
        return citizens.values();
    }

    public int getCitizenCount() {
        return citizens.size();
    }

    public boolean isOwner(UUID uuid) {
        return owner.equals(uuid);
    }

    public boolean canAccess(EntityPlayer player) {
        return isOwner(player.getUniqueID());
    }

    public void enqueueOrder(CitizenCommand command) {
        orders.addLast(command);
    }

    public CitizenCommand pollOrder() {
        return orders.pollFirst();
    }

    public CitizenCommand pollOrderFor(EntityCitizen citizen) {
        for (CitizenCommand order : orders) {
            if (order.canBeClaimedBy(citizen)) {
                orders.remove(order);
                return order;
            }
        }
        return null;
    }

    public int getOrderCount() {
        return orders.size();
    }

    public int clearOrders() {
        int cleared = orders.size();
        orders.clear();
        return cleared;
    }

    public int clearOrders(String group) {
        String target = group == null ? "" : group;
        int cleared = 0;
        Iterator<CitizenCommand> iterator = orders.iterator();
        while (iterator.hasNext()) {
            if (target.equals(
                iterator.next()
                    .getTargetGroup())) {
                iterator.remove();
                cleared++;
            }
        }
        return cleared;
    }

    public int getOrderCount(String group) {
        String target = group == null ? "" : group;
        int count = 0;
        for (CitizenCommand order : orders) {
            if (target.equals(order.getTargetGroup())) {
                count++;
            }
        }
        return count;
    }

    public boolean isCenteredAt(int dimension, int x, int y, int z) {
        return this.dimension == dimension && this.x == x && this.y == y && this.z == z;
    }

    public boolean isInside(int dimension, double x, double z) {
        if (this.dimension != dimension) {
            return false;
        }
        double dx = this.x + 0.5D - x;
        double dz = this.z + 0.5D - z;
        return dx * dx + dz * dz <= (double) Config.colonyRadius * Config.colonyRadius;
    }

    public double distanceSqTo(int dimension, int x, int z) {
        if (this.dimension != dimension) {
            return Double.MAX_VALUE;
        }
        double dx = this.x - x;
        double dz = this.z - z;
        return dx * dx + dz * dz;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("id", id);
        tag.setString("name", name);
        tag.setString("owner", owner.toString());
        tag.setString("ownerName", ownerName);
        tag.setInteger("dim", dimension);
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
        for (ColonySiteKind kind : ColonySiteKind.values()) {
            sites.get(kind)
                .writeToNBT(tag, kind);
        }

        NBTTagList blueprintList = new NBTTagList();
        for (Blueprint entry : blueprints) {
            blueprintList.appendTag(entry.writeToNBT());
        }
        tag.setTag("blueprints", blueprintList);
        tag.setInteger("activeBlueprint", activeBlueprint);
        tag.setInteger("placeRotation", placeRotation);
        tag.setBoolean("placeMirror", placeMirror);
        if (buildSite != null) {
            tag.setTag("buildSite", buildSite.writeToNBT());
        }

        NBTTagList homeList = new NBTTagList();
        for (ColonyHome home : homes) {
            homeList.appendTag(home.writeToNBT());
        }
        tag.setTag("homes", homeList);
        tag.setInteger("nextHomeId", nextHomeId);

        NBTTagList orderList = new NBTTagList();
        for (CitizenCommand order : orders) {
            orderList.appendTag(CitizenCommandRegistry.write(order));
        }
        tag.setTag("orders", orderList);

        NBTTagList citizenList = new NBTTagList();
        for (ColonyCitizen citizen : citizens.values()) {
            citizenList.appendTag(citizen.writeToNBT());
        }
        tag.setTag("citizens", citizenList);
        return tag;
    }

    public static Colony readFromNBT(NBTTagCompound tag) {
        Colony colony = new Colony();
        colony.id = tag.getInteger("id");
        colony.name = tag.getString("name");
        colony.owner = UUID.fromString(tag.getString("owner"));
        colony.ownerName = tag.getString("ownerName");
        colony.dimension = tag.getInteger("dim");
        colony.x = tag.getInteger("x");
        colony.y = tag.getInteger("y");
        colony.z = tag.getInteger("z");
        for (ColonySiteKind kind : ColonySiteKind.values()) {
            colony.sites.get(kind)
                .readFromNBT(tag, kind);
        }

        if (tag.hasKey("blueprint", 10)) {
            Blueprint legacy = Blueprint.readFromNBT(tag.getCompoundTag("blueprint"));
            if (legacy != null) {
                legacy.setName("blueprint");
                colony.blueprints.add(legacy);
            }
        }
        NBTTagList blueprintList = tag.getTagList("blueprints", 10);
        for (int i = 0; i < blueprintList.tagCount() && colony.blueprints.size() < MAX_BLUEPRINTS; i++) {
            Blueprint entry = Blueprint.readFromNBT(blueprintList.getCompoundTagAt(i));
            if (entry != null) {
                colony.blueprints.add(entry);
            }
        }
        colony.activeBlueprint = tag.hasKey("activeBlueprint") ? tag.getInteger("activeBlueprint")
            : colony.blueprints.isEmpty() ? -1 : 0;
        colony.placeRotation = tag.getInteger("placeRotation");
        colony.placeMirror = tag.getBoolean("placeMirror");
        if (tag.hasKey("buildSite", 10)) {
            colony.buildSite = BuildSite.readFromNBT(tag.getCompoundTag("buildSite"));
        }

        NBTTagList homeList = tag.getTagList("homes", 10);
        for (int i = 0; i < homeList.tagCount() && colony.homes.size() < MAX_HOMES; i++) {
            colony.homes.add(ColonyHome.readFromNBT(homeList.getCompoundTagAt(i)));
        }
        colony.nextHomeId = Math.max(tag.getInteger("nextHomeId"), 1);
        for (ColonyHome home : colony.homes) {
            colony.nextHomeId = Math.max(colony.nextHomeId, home.getId() + 1);
        }

        NBTTagList orderList = tag.getTagList("orders", 10);
        for (int i = 0; i < orderList.tagCount(); i++) {
            CitizenCommand order = CitizenCommandRegistry.read(orderList.getCompoundTagAt(i));
            if (order != null) {
                colony.orders.addLast(order);
            }
        }

        NBTTagList citizenList = tag.getTagList("citizens", 10);
        for (int i = 0; i < citizenList.tagCount(); i++) {
            ColonyCitizen citizen = ColonyCitizen.readFromNBT(citizenList.getCompoundTagAt(i));
            colony.citizens.put(citizen.getId(), citizen);
        }
        return colony;
    }

    @Override
    public String toString() {
        return "Colony#" + id
            + "["
            + name
            + ", owner="
            + ownerName
            + ", dim="
            + dimension
            + ", "
            + x
            + "/"
            + y
            + "/"
            + z
            + "]";
    }
}
