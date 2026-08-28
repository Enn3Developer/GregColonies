package com.enn3developer.gregcolonies.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import com.enn3developer.gregcolonies.entity.ai.WorkPhase;

class CitizenEnumsTest {

    @Test
    void jobIdsAreStableAcrossSaves() {
        assertEquals(0, CitizenJob.NONE.getId());
        assertEquals(1, CitizenJob.BUILDER.getId());
    }

    @Test
    void unknownJobIdsFallBackToNone() {
        assertEquals(CitizenJob.BUILDER, CitizenJob.byId(1));
        assertEquals(CitizenJob.NONE, CitizenJob.byId(99));
        assertEquals(CitizenJob.NONE, CitizenJob.byId(-1));
    }

    @Test
    void everyJobRoundTripsThroughItsId() {
        for (CitizenJob job : CitizenJob.values()) {
            assertEquals(job, CitizenJob.byId(CitizenJob.idOf(job)));
        }
        assertEquals(CitizenJob.NONE.getId(), CitizenJob.idOf(null));
    }

    @Test
    void jobsResolveByName() {
        assertEquals(CitizenJob.BUILDER, CitizenJob.byName("BUILDER"));
        assertEquals(CitizenJob.NONE, CitizenJob.byName("builder"));
        assertEquals(CitizenJob.NONE, CitizenJob.byName("nonsense"));
    }

    @Test
    void jobsHaveLabels() {
        assertEquals("no job", CitizenJob.NONE.getLabel());
        assertEquals("builder", CitizenJob.BUILDER.getLabel());
    }

    @Test
    void genderIdsLeaveZeroFreeForUnknown() {
        assertEquals(1, CitizenGender.MALE.getId());
        assertEquals(2, CitizenGender.FEMALE.getId());
        assertEquals(0, CitizenGender.idOf(null));
        assertNull(CitizenGender.byId(0));
    }

    @Test
    void everyGenderRoundTripsThroughItsId() {
        for (CitizenGender gender : CitizenGender.values()) {
            assertEquals(gender, CitizenGender.byId(CitizenGender.idOf(gender)));
        }
        assertNull(CitizenGender.byId(99));
    }

    @Test
    void genderIsDescribedWithChildhood() {
        assertEquals("male", CitizenGender.describe(CitizenGender.MALE, false));
        assertEquals("female child", CitizenGender.describe(CitizenGender.FEMALE, true));
        assertEquals("child", CitizenGender.describe(null, true));
        assertEquals("", CitizenGender.describe(null, false));
    }

    @Test
    void randomGenderProducesBoth() {
        Random random = new Random(1234L);
        Set<CitizenGender> seen = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            seen.add(CitizenGender.random(random));
        }
        assertEquals(2, seen.size());
    }

    @Test
    void workPhaseIdsAreStableAcrossSaves() {
        assertEquals(0, WorkPhase.TRAVEL.id());
        assertEquals(1, WorkPhase.WORK.id());
        assertEquals(2, WorkPhase.FINISH.id());
    }

    @Test
    void everyWorkPhaseRoundTripsThroughItsId() {
        for (WorkPhase phase : WorkPhase.values()) {
            assertEquals(phase, WorkPhase.byId(phase.id()));
        }
    }

    @Test
    void unknownWorkPhasesFallBackToTravel() {
        assertEquals(WorkPhase.TRAVEL, WorkPhase.byId(-1));
        assertEquals(WorkPhase.TRAVEL, WorkPhase.byId(99));
    }

    @Test
    void citizenParametersRoundTripTheGender() {
        CitizenParameters parameters = new CitizenParameters();
        parameters.setGender(CitizenGender.FEMALE);

        NBTTagCompound tag = new NBTTagCompound();
        parameters.writeToNBT(tag);

        CitizenParameters read = new CitizenParameters();
        read.readFromNBT(tag);
        assertEquals(CitizenGender.FEMALE, read.getGender());
    }

    @Test
    void citizenParametersSurviveAnUnsetGender() {
        CitizenParameters parameters = new CitizenParameters();
        NBTTagCompound tag = new NBTTagCompound();
        parameters.writeToNBT(tag);

        CitizenParameters read = new CitizenParameters();
        read.readFromNBT(tag);
        assertNull(read.getGender());
    }
}
