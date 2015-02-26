package com.mv2studio.amarok.kontrol.communication;

/**
 * Created by matej on 17.11.14.
 */
public interface Command {

    public String getCommand();

    /**
     * Execute specific command.
     * Use @link com.mv2studio.amarok.kontrol.communication.Connector#sendCommand() for general commands.
     */
    public void execute();

    public void execute(String params);

    public Command withCallback(CommandCallback callback);
}
