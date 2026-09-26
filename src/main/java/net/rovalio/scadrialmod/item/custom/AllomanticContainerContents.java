package net.rovalio.scadrialmod.item.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.rovalio.scadrialmod.power.allomancy.AllomanticFuel;

import java.util.ArrayList;
import java.util.List;

public record AllomanticContainerContents(List<Portion> portions) {

    private static final Codec<AllomanticFuel> FUEL_CODEC =
            Codec.STRING.comapFlatMap(
                    AllomanticContainerContents::readFuel,
                    AllomanticFuel::getSerializedName
            );

    public static final AllomanticContainerContents EMPTY =
            new AllomanticContainerContents(List.of());

    public static final Codec<AllomanticContainerContents> CODEC =
            Portion.CODEC.listOf().xmap(
                    AllomanticContainerContents::new,
                    AllomanticContainerContents::portions
            );

    public AllomanticContainerContents {
        portions = List.copyOf(portions);
    }

    public boolean isEmpty() {
        return portions.isEmpty();
    }

    public AllomanticContainerContents withAdded(
            AllomanticFuel fuel,
            Form form,
            long amount
    ) {
        List<Portion> updated = new ArrayList<>(portions);

        for (int i = 0; i < updated.size(); i++) {
            Portion existing = updated.get(i);

            if (existing.fuel() == fuel && existing.form() == form) {
                updated.set(i, new Portion(
                        fuel,
                        form,
                        Math.addExact(existing.subunits(), amount)
                ));
                return new AllomanticContainerContents(updated);
            }
        }

        updated.add(new Portion(fuel, form, amount));
        return new AllomanticContainerContents(updated);
    }

    public AllomanticContainerContents withRemoved(
            int index,
            long amount
    ) {
        List<Portion> updated = new ArrayList<>(portions);
        Portion existing = updated.get(index);

        if (amount <= 0 || amount > existing.subunits()) {
            throw new IllegalArgumentException("Invalid amount to remove");
        }

        if (amount == existing.subunits()) {
            updated.remove(index);
        } else {
            updated.set(index, new Portion(
                    existing.fuel(),
                    existing.form(),
                    existing.subunits() - amount
            ));
        }

        return new AllomanticContainerContents(updated);
    }

    private static DataResult<AllomanticFuel> readFuel(String name) {
        return AllomanticFuel.fromSerializedName(name)
                .<DataResult<AllomanticFuel>>map(DataResult::success)
                .orElseGet(() -> DataResult.error(
                        () -> "Unknown allomantic fuel: " + name
                ));
    }

    public enum Form {
        DUST("dust", 250, 4),
        SHAVINGS("shavings", 1_000, 3),
        BEAD("bead", 2_000, 2);

        public static final Codec<Form> CODEC =
                Codec.STRING.comapFlatMap(
                        Form::read,
                        Form::serializedName
                );

        private final String serializedName;
        private final long subunitsPerItem;

        private final long spaceCostPerSubunit;

        Form(
                String serializedName,
                long subunitsPerItem,
                long spaceCostPerSubunit
        ) {
            this.serializedName = serializedName;
            this.subunitsPerItem = subunitsPerItem;
            this.spaceCostPerSubunit = spaceCostPerSubunit;
        }

        public String serializedName() {
            return serializedName;
        }

        public long subunitsPerItem() {
            return subunitsPerItem;
        }

        public long spaceCostPerSubunit() {
            return spaceCostPerSubunit;
        }

        private static DataResult<Form> read(String name) {
            for (Form form : values()) {
                if (form.serializedName.equals(name)) {
                    return DataResult.success(form);
                }
            }

            return DataResult.error(
                    () -> "Unknown metal form: " + name
            );
        }
    }

    public record Portion(
            AllomanticFuel fuel,
            Form form,
            long subunits
    ) {
        public static final Codec<Portion> CODEC =
                RecordCodecBuilder.create(instance ->
                        instance.group(
                                FUEL_CODEC.fieldOf("fuel")
                                        .forGetter(Portion::fuel),
                                Form.CODEC.fieldOf("form")
                                        .forGetter(Portion::form),
                                Codec.LONG.fieldOf("subunits")
                                        .forGetter(Portion::subunits)
                        ).apply(instance, Portion::new)
                );
    }
}