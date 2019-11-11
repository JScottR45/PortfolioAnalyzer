package controllers;

/**
 * Created by scottreese on 6/3/19.
 *
 * The interface which all UI controllers must implement.
 */
public interface Controller {

    /**
     * Initializes the UI controller. Set up member variables and kick off any processes needed to run the application
     * component associated with the controller.
     */
    void initialize();
}
