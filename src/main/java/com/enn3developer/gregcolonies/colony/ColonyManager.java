package com.enn3developer.gregcolonies.colony;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

import com.enn3developer.gregcolonies.entity.CitizenJob;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommand;

public class ColonyManager extends WorldSavedData {

    public static final String DATA_NAME = "gregcolonies_colonies";

    private final Map<Integer, Colony> colonies = new LinkedHashMap<>();
    private int nextId = 1;

    public ColonyManager() {
        super(DATA_NAME);
    }

    public ColonyManager(String name) {
        super(name);
    }

    public static ColonyManager get(World world) {
        MapStorage storage = world.mapStorage;
        ColonyManager manager = (ColonyManager) storage.loadData(ColonyManager.class, DATA_NAME);
        if (manager == null) {
            manager = new ColonyManager();
            storage.setData(DATA_NAME, manager);
        }
        return manager;
    }

    public Collection<Colony> getColonies() {
        return Collections.unmodifiableCollection(colonies.values());
    }

    public int getColonyCount() {
        return colonies.size();
    }

    public Colony getColony(int id) {
        return colonies.get(id);
    }

    public Colony getColonyAt(int dimension, int x, int y, int z) {
        for (Colony colony : colonies.values()) {
            if (colony.isCenteredAt(dimension, x, y, z)) {
                return colony;
            }
        }
        return null;
    }

    public Colony getNearestColonyOf(UUID owner, int dimension, int x, int z) {
        Colony nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Colony colony : colonies.values()) {
            if (!colony.isOwner(owner)) {
                continue;
            }
            double distance = colony.distanceSqTo(dimension, x, z);
            if (distance < nearestDistance) {
                nearest = colony;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    public Colony getNearestColony(int dimension, int x, int z) {
        Colony nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Colony colony : colonies.values()) {
            double distance = colony.distanceSqTo(dimension, x, z);
            if (distance < nearestDistance) {
                nearest = colony;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    public Colony createColony(String name, UUID owner, String ownerName, int dimension, int x, int y, int z) {
        Colony colony = new Colony(nextId++, name, owner, ownerName, dimension, x, y, z);
        colonies.put(colony.getId(), colony);
        markDirty();
        return colony;
    }

    private boolean mutate(int colonyId, Predicate<Colony> change) {
        Colony colony = colonies.get(colonyId);
        if (colony == null || !change.test(colony)) {
            return false;
        }
        markDirty();
        return true;
    }

    public ColonyCitizen registerCitizen(int colonyId, EntityCitizen citizen) {
        Colony colony = colonies.get(colonyId);
        if (colony == null) {
            return null;
        }
        ColonyCitizen entry = colony.registerCitizen(citizen);
        markDirty();
        return entry;
    }

    public boolean removeCitizen(int colonyId, UUID id) {
        return mutate(colonyId, colony -> colony.removeCitizen(id));
    }

    public boolean setCitizenGroup(int colonyId, UUID id, String group) {
        return mutate(colonyId, colony -> {
            ColonyCitizen entry = colony.getCitizen(id);
            if (entry == null) {
                return false;
            }
            entry.setGroup(group);
            return true;
        });
    }

    public boolean setCitizenJob(int colonyId, UUID id, CitizenJob job) {
        return mutate(colonyId, colony -> {
            ColonyCitizen entry = colony.getCitizen(id);
            if (entry == null) {
                return false;
            }
            entry.setJob(job);
            return true;
        });
    }

    public boolean setSite(int colonyId, ColonySiteKind kind, int x, int y, int z) {
        return mutate(colonyId, colony -> {
            colony.site(kind)
                .set(x, y, z);
            return true;
        });
    }

    public boolean clearSite(int colonyId, ColonySiteKind kind) {
        return mutate(colonyId, colony -> {
            ColonySite site = colony.site(kind);
            if (!site.isPresent()) {
                return false;
            }
            site.clear();
            return true;
        });
    }

    public int addBlueprint(int colonyId, Blueprint blueprint) {
        Colony colony = colonies.get(colonyId);
        if (colony == null) {
            return -1;
        }
        int index = colony.addBlueprint(blueprint);
        if (index >= 0) {
            markDirty();
        }
        return index;
    }

    public boolean replaceBlueprint(int colonyId, int index, Blueprint blueprint) {
        return mutate(colonyId, colony -> colony.replaceBlueprint(index, blueprint));
    }

    public boolean removeBlueprint(int colonyId, int index) {
        return mutate(colonyId, colony -> colony.removeBlueprint(index));
    }

    public boolean renameBlueprint(int colonyId, int index, String name) {
        return mutate(colonyId, colony -> colony.renameBlueprint(index, name));
    }

    public boolean setActiveBlueprint(int colonyId, int index) {
        return mutate(colonyId, colony -> colony.setActiveBlueprint(index));
    }

    public boolean setPlacement(int colonyId, int rotation, boolean mirror) {
        return mutate(colonyId, colony -> {
            colony.setPlaceRotation(rotation);
            colony.setPlaceMirror(mirror);
            return true;
        });
    }

    public boolean setBuildSite(int colonyId, BuildSite site) {
        return mutate(colonyId, colony -> {
            colony.setBuildSite(site);
            return true;
        });
    }

    public boolean claimBuildSite(int colonyId, UUID id, long time) {
        Colony colony = colonies.get(colonyId);
        return colony != null && colony.claimBuildSite(id, time);
    }

    public void releaseBuildSite(int colonyId, UUID id) {
        Colony colony = colonies.get(colonyId);
        if (colony != null) {
            colony.releaseBuildSite(id);
        }
    }

    public boolean claimBed(int colonyId, UUID id, int x, int y, int z) {
        return mutate(colonyId, colony -> colony.claimBed(id, x, y, z));
    }

    public void releaseBed(int colonyId, UUID id) {
        mutate(colonyId, colony -> {
            colony.releaseBed(id);
            return true;
        });
    }

    public boolean enqueueOrder(int colonyId, CitizenCommand command) {
        return mutate(colonyId, colony -> {
            colony.enqueueOrder(command);
            return true;
        });
    }

    public CitizenCommand pollOrder(int colonyId, EntityCitizen citizen) {
        Colony colony = colonies.get(colonyId);
        if (colony == null) {
            return null;
        }
        CitizenCommand order = colony.pollOrderFor(citizen);
        if (order != null) {
            markDirty();
        }
        return order;
    }

    public int clearOrders(int colonyId) {
        Colony colony = colonies.get(colonyId);
        if (colony == null) {
            return 0;
        }
        int cleared = colony.clearOrders();
        if (cleared > 0) {
            markDirty();
        }
        return cleared;
    }

    public int clearOrders(int colonyId, String group) {
        Colony colony = colonies.get(colonyId);
        if (colony == null) {
            return 0;
        }
        int cleared = colony.clearOrders(group);
        if (cleared > 0) {
            markDirty();
        }
        return cleared;
    }

    public boolean removeColony(int id) {
        if (colonies.remove(id) == null) {
            return false;
        }
        markDirty();
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        colonies.clear();
        nextId = tag.getInteger("nextId");
        if (nextId < 1) {
            nextId = 1;
        }
        NBTTagList list = tag.getTagList("colonies", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            Colony colony = Colony.readFromNBT(list.getCompoundTagAt(i));
            colonies.put(colony.getId(), colony);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("nextId", nextId);
        NBTTagList list = new NBTTagList();
        for (Colony colony : colonies.values()) {
            list.appendTag(colony.writeToNBT());
        }
        tag.setTag("colonies", list);
    }
}
