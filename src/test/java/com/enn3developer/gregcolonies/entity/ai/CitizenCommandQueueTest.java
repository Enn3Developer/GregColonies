package com.enn3developer.gregcolonies.entity.ai;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enn3developer.gregcolonies.testing.MinecraftBootstrap;
import com.enn3developer.gregcolonies.testing.TestCommand;

class CitizenCommandQueueTest {

    private CitizenCommandQueue queue;

    @BeforeAll
    static void boot() {
        MinecraftBootstrap.ensure();
        TestCommand.ensureRegistered();
    }

    @BeforeEach
    void freshQueue() {
        queue = new CitizenCommandQueue();
    }

    @Test
    void anEmptyQueueHasNoWork() {
        assertFalse(queue.hasWork());
        assertEquals(0, queue.getPendingCount());
        assertNull(queue.getCurrent());
    }

    @Test
    void enqueueingGivesTheQueueWork() {
        queue.enqueue(new TestCommand(1));
        assertTrue(queue.hasWork());
        assertEquals(1, queue.getPendingCount());
    }

    @Test
    void enqueueNextJumpsTheLine() {
        queue.enqueue(new TestCommand(1));
        queue.enqueueNext(new TestCommand(2));

        NBTTagCompound tag = new NBTTagCompound();
        queue.writeToNBT(tag);
        assertEquals(
            2,
            ((TestCommand) CitizenCommandRegistry.read(
                tag.getTagList("pending", 10)
                    .getCompoundTagAt(0))).getPayload());
    }

    @Test
    void anEmptyQueueFearsEnemiesAndAllowsSleep() {
        assertTrue(queue.fearsEnemies());
        assertTrue(queue.allowsSleep());
    }

    @Test
    void theHeadOfTheQueueDecidesFearAndSleep() {
        queue.enqueue(new TestCommand());
        assertTrue(queue.fearsEnemies());
        assertTrue(queue.allowsSleep());

        CitizenCommandQueue brave = new CitizenCommandQueue();
        brave.enqueue(new CitizenCommand() {

            @Override
            public String getId() {
                return TestCommand.ID;
            }

            @Override
            public boolean fearsEnemies() {
                return false;
            }

            @Override
            public boolean allowsSleep() {
                return false;
            }

            @Override
            public CitizenCommandResult update(com.enn3developer.gregcolonies.entity.EntityCitizen citizen) {
                return CitizenCommandResult.RUNNING;
            }
        });
        assertFalse(brave.fearsEnemies());
        assertFalse(brave.allowsSleep());
    }

    @Test
    void clearingAnIdleQueueIsSafe() {
        queue.enqueue(new TestCommand());
        queue.clear(null);
        assertFalse(queue.hasWork());
        assertEquals(0, queue.getPendingCount());
    }

    @Test
    void nbtRoundTripsThePendingQueue() {
        queue.enqueue(new TestCommand(11));
        queue.enqueue(new TestCommand(22));

        NBTTagCompound tag = new NBTTagCompound();
        queue.writeToNBT(tag);

        CitizenCommandQueue read = new CitizenCommandQueue();
        read.readFromNBT(tag);

        assertEquals(2, read.getPendingCount());
        assertTrue(read.hasWork());
    }

    @Test
    void readingReplacesWhateverWasQueued() {
        queue.enqueue(new TestCommand(1));
        queue.readFromNBT(new NBTTagCompound());

        assertFalse(queue.hasWork());
        assertEquals(0, queue.getPendingCount());
    }

    @Test
    void unknownCommandsAreDroppedNotFatal() {
        queue.enqueue(new TestCommand(1));
        NBTTagCompound tag = new NBTTagCompound();
        queue.writeToNBT(tag);
        tag.getTagList("pending", 10)
            .getCompoundTagAt(0)
            .setString("id", "gregcolonies:removed_in_a_later_version");

        CitizenCommandQueue read = new CitizenCommandQueue();
        read.readFromNBT(tag);
        assertEquals(0, read.getPendingCount());
    }

    @Test
    void theTargetGroupSurvivesTheRegistry() {
        TestCommand command = new TestCommand(5);
        command.setTargetGroup("alpha");

        CitizenCommand read = CitizenCommandRegistry.read(CitizenCommandRegistry.write(command));
        assertNotNull(read);
        assertEquals("alpha", read.getTargetGroup());
        assertEquals(5, ((TestCommand) read).getPayload());
    }

    @Test
    void theRegistryRefusesDuplicateIds() {
        assertThrows(
            IllegalArgumentException.class,
            () -> CitizenCommandRegistry.register(TestCommand.ID, TestCommand::new));
    }

    @Test
    void theRegistryReturnsNullForUnknownIds() {
        assertNull(CitizenCommandRegistry.create("gregcolonies:nope"));
    }

    @Test
    void aCommandDescribesItselfByItsIdByDefault() {
        assertEquals(TestCommand.ID, new TestCommand().describe());
    }
}
