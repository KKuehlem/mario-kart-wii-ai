package de.minekonst.mariokartwiiai.shared.utils.commandline;

import de.minekonst.mariokartwiiai.shared.utils.dynamictype.TypeFinder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CommandLineSystem {

    private final List<CommandLineFlag> flags;
    private final List<CommandLineParameter<?>> paramters;
    private boolean scaned;
    private final Logger logger;

    public CommandLineSystem(Logger logger) {
        flags = new ArrayList<>();
        paramters = new ArrayList<>();
        this.logger = logger;
    }

    public void addFlags(CommandLineFlag... flags) {
        for (CommandLineFlag f : flags) {
            checkNaming(f);
            this.flags.add(f);
        }
    }

    public void addParamter(CommandLineParameter<?>... parameters) {
        for (CommandLineParameter<?> p : parameters) {
            if (!TypeFinder.supports(p.getType())) {
                throw new CommandLineException("Data Type " + p.getType().getSimpleName() + " not supported (See TypeFinder)");
            }

            checkNaming(p);
            this.paramters.add(p);
        }
    }

    /**
     * Scan the commandline
     *
     * @param args The commandline (will be splitted by \s+)
     *
     * @return true, if no problem opccurred
     */
    public boolean scan(String args) {
        return scan(args.split("\\s+"));
    }

    /**
     * Scan the commandline
     *
     * @param args The commandline
     *
     * @return true, if no problem opccurred
     */
    public boolean scan(String[] args) {
        if (scaned) {
            throw new CommandLineException("Already scaned");
        }
        scaned = true;

        flags.forEach(CommandLineFlag::onReady);
        paramters.forEach(CommandLineParameter::onReady);

        outer:
        for (int x = 0; x < args.length; x++) {
            String s = args[x];
            if (s.isBlank()) {
                continue;
            }
            if (!s.startsWith("-")) {
                logger.warning(String.format("Unexpeted start of option: %s", s));
                return false;
            }

            if (s.equals("-h") || s.equals("--help")) {
                showHelp();
                return false;
            }

            for (CommandLineFlag f : flags) {
                if (s.equals("-" + f.getShortName()) || s.equals("--" + f.getLongName())) {
                    if (f.isSet()) {
                        logger.warning(String.format("Flag %s set multiple times", f.toString()));
                        return false;
                    }
                    f.set();
                    continue outer;
                }
            }

            for (CommandLineParameter<?> p : paramters) {
                if (s.equals("-" + p.getShortName()) || s.equals("--" + p.getLongName())) {
                    if (p.getValue() != null) {
                        logger.warning(String.format( "Paramter %s set multiple times", p.toString()));
                        return false;
                    }

                    x++;
                    if (x < args.length) {
                        String val = args[x];
                        Object parse = TypeFinder.parse(val, p.getType());
                        if (parse != null) {
                            p.setValue(parse);
                            continue outer;
                        }
                        else {
                            logger.warning(String.format( "Cannot read paramter %s: %s cannot be converted to %s",
                                    p.toString(), val, p.getType().getSimpleName()));
                            return false;
                        }
                    }
                    else {
                        logger.warning(String.format("Paramter %s mentioned without a value", p.toString()));
                        return false;
                    }
                }
            }

            logger.warning(String.format("Unknown option: %s (--help for a list of options)", s));
            return false;
        }

        for (CommandLineParameter<?> p : paramters) {
            if (p.checkRequierd()) {
                logger.warning(String.format("Requiered paramter \"%s\" not set", p.toString()));
                return false;
            }
        }

        return true;
    }

    private void checkNaming(CommandLineOption o) {
        for (CommandLineFlag f : flags) {
            checkNameClash(f, o);
        }
        for (CommandLineParameter<?> p : paramters) {
            checkNameClash(p, o);
        }
    }

    private void checkNameClash(CommandLineOption a, CommandLineOption b) {
        if (a.getLongName() != null) {
            if (a.getLongName().equals(b.getLongName())) {
                throw new CommandLineException("Option with long name " + a.getLongName() + " is already registered");
            }
        }

        if (a.getShortName() != null) {
            if (a.getShortName().equals(b.getShortName())) {
                throw new CommandLineException("Option with short name " + a.getShortName() + " is already registered");
            }
        }
    }

    private void showHelp() {
        flags.sort((CommandLineFlag a, CommandLineFlag b) -> {
            return a.getLongName().compareTo(b.getLongName());
        });
        paramters.sort((CommandLineParameter<?> a, CommandLineParameter<?> b) -> {
            return a.getLongName().compareTo(b.getLongName());
        });

        if (!flags.isEmpty()) {
            logger.info("=== Flags ===");
            for (CommandLineFlag f : flags) {
                logger.info(String.format( "--%s / -%s: %s", f.getLongName(), f.getShortName(), f.getDescription()));
            }
        }

        if (!paramters.isEmpty()) {
            logger.info("=== Paramters ===");
            for (CommandLineParameter<?> p : paramters) {
                logger.info(String.format("--%s /-%s <%s>: %s%s", p.getLongName(), p.getShortName(),
                        p.getType().getSimpleName(),
                        p.getDescription(), p.isRequiered() ? " (Requiered)" : ""));
            }
        }
    }
}
