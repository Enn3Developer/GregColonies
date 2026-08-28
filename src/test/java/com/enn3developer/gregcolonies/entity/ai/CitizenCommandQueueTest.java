package com.enn3developer.gregcolonies.entity.ai;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.testing.MinecraftBootstrap;
import com.enn3developer.gregcolonies.testing.TestCommand;

class CitizenCommandQueueTest {

    private CitizenCommandQueue queue;

    @BeforeAll
    static void boot() {
        MinecraftBootstrap.ensure();
        TestCommand.ensureRegistered();
        ScriptedCommand.ensureRegistered();
    }

    private static final class ScriptedCommand extends CitizenCommand {

        static final String ID = "gregcolonies:scripted";

        private static boolean registered;

        private int runsLeft;

        private int started;

        private int finished;

        private boolean bold;

        static synchronized void ensureRegistered() {
            if (registered) {
                return;
            }
            CitizenCommandRegistry.register(ID, () -> new ScriptedCommand(1));
            registered = true;
        }

        ScriptedCommand(int runsLeft) {
            this.runsLeft = runsLeft;
        }

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public boolean fearsEnemies() {
            return !bold;
        }

        @Override
        public boolean allowsSleep() {
            return !bold;
        }

        @Override
        public void start(EntityCitizen citizen) {
            started++;
        }

        @Override
        public CitizenCommandResult update(EntityCitizen citizen) {
            return --runsLeft > 0 ? CitizenCommandResult.RUNNING : CitizenCommandResult.DONE;
        }

        @Override
        public void finish(EntityCitizen citizen) {
            finished++;
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            tag.setInteger("runsLeft", runsLeft);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            runsLeft = tag.getInteger("runsLeft");
        }
    }

    private static int payloadAt(NBTTagCompound tag, int index) {
        return ((TestCommand) CitizenCommandRegistry.read(
            tag.getTagList("pending", 10)
                .getCompoundTagAt(index))).getPayload();
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
        assertEquals(2, payloadAt(tag, 0));
        assertEquals(1, payloadAt(tag, 1));
    }

    @Test
    void updatingAnEmptyQueueDoesNothing() {
        queue.update(null);
        assertNull(queue.getCurrent());
        assertFalse(queue.hasWork());
    }

    @Test
    void updatingStartsTheHeadOfTheQueueExactlyOnce() {
        ScriptedCommand command = new ScriptedCommand(3);
        queue.enqueue(command);

        queue.update(null);
        assertSame(command, queue.getCurrent());
        assertEquals(1, command.started);
        assertEquals(0, queue.getPendingCount(), "the running command leaves the pending queue");

        queue.update(null);
        assertSame(command, queue.getCurrent());
        assertEquals(1, command.started, "a running command must not be restarted");
        assertEquals(0, command.finished);
    }

    @Test
    void aFinishedCommandMakesWayForTheNext() {
        ScriptedCommand first = new ScriptedCommand(1);
        ScriptedCommand second = new ScriptedCommand(1);
        queue.enqueue(first);
        queue.enqueue(second);

        queue.update(null);
        assertEquals(1, first.finished);
        assertNull(queue.getCurrent());
        assertTrue(queue.hasWork());

        queue.update(null);
        assertEquals(1, second.finished);
        assertFalse(queue.hasWork());
    }

    @Test
    void clearingFinishesTheCommandInFlight() {
        ScriptedCommand command = new ScriptedCommand(5);
        queue.enqueue(command);
        queue.update(null);

        queue.clear(null);
        assertEquals(1, command.finished);
        assertNull(queue.getCurrent());
        assertFalse(queue.hasWork());
    }

    @Test
    void clearingDoesNotFinishACommandThatNeverStarted() {
        ScriptedCommand command = new ScriptedCommand(5);
        queue.enqueue(command);

        queue.clear(null);
        assertEquals(0, command.started);
        assertEquals(0, command.finished);
    }

    @Test
    void theCommandInFlightDecidesFearAndSleep() {
        ScriptedCommand running = new ScriptedCommand(5);
        running.bold = true;
        queue.enqueue(running);
        queue.update(null);
        queue.enqueue(new TestCommand());

        assertSame(running, queue.getCurrent());
        assertFalse(queue.fearsEnemies(), "the command in flight wins over the one still waiting");
        assertFalse(queue.allowsSleep());
    }

    @Test
    void theCommandInFlightSurvivesNbt() {
        queue.enqueue(new ScriptedCommand(5));
        queue.enqueue(new TestCommand(9));
        queue.update(null);
        assertNotNull(queue.getCurrent());

        NBTTagCompound tag = new NBTTagCompound();
        queue.writeToNBT(tag);

        CitizenCommandQueue read = new CitizenCommandQueue();
        read.readFromNBT(tag);

        assertNotNull(read.getCurrent());
        assertEquals(
            ScriptedCommand.ID,
            read.getCurrent()
                .getId());
        assertEquals(1, read.getPendingCount());
        assertEquals(9, payloadAt(tag, 0));
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

        NBTTagCompound again = new NBTTagCompound();
        read.writeToNBT(again);
        assertEquals(11, payloadAt(again, 0), "the queue order must survive the save");
        assertEquals(22, payloadAt(again, 1));
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
    void aRejectedDuplicateLeavesTheOriginalFactoryInPlace() {
        assertThrows(
            IllegalArgumentException.class,
            () -> CitizenCommandRegistry.register(TestCommand.ID, () -> new ScriptedCommand(1)));
        assertInstanceOf(TestCommand.class, CitizenCommandRegistry.create(TestCommand.ID));
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
