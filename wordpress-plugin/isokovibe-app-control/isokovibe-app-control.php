<?php
/**
 * Plugin Name: iSokoVibe App Control
 * Description: Controls the iSokoVibe Music Player app from WordPress — send announcements to phones, prompt users to update, and manage the in-app promo banners.
 * Version:     1.0.0
 * Author:      iSokoVibe
 * License:     GPL-2.0-or-later
 *
 * Everything the app needs is served from ONE public REST endpoint:
 *
 *     GET /wp-json/isokovibe/v1/app-config
 *
 * The app polls that single URL rather than three separate ones, so a phone
 * on a bad connection makes one request instead of three, and the three
 * features can never be read in inconsistent states.
 *
 * The endpoint is intentionally public and read-only. The app ships to
 * ordinary users and has no credentials to protect, and everything here is
 * content you are choosing to broadcast anyway.
 */

if (!defined('ABSPATH')) {
    exit;
}

define('ISOKOVIBE_APP_CONTROL_VERSION', '1.0.0');

/* -------------------------------------------------------------------------
 * Content types
 * ---------------------------------------------------------------------- */

add_action('init', 'isokovibe_app_control_register_types');
function isokovibe_app_control_register_types()
{
    // Announcements: one per thing you want to tell users about. Publishing
    // one is what makes phones show a notification — nothing else on the
    // site triggers it, so ordinary posts never reach the app.
    register_post_type('isokovibe_announce', array(
        'labels' => array(
            'name'               => 'App Announcements',
            'singular_name'      => 'Announcement',
            'add_new'            => 'New Announcement',
            'add_new_item'       => 'New Announcement',
            'edit_item'          => 'Edit Announcement',
            'menu_name'          => 'App Control',
            'not_found'          => 'No announcements sent yet.',
        ),
        'public'        => false,
        'show_ui'       => true,
        'menu_icon'     => 'dashicons-megaphone',
        'menu_position' => 26,
        'supports'      => array('title', 'editor'),
    ));

    // Banners: the 330x70 promo slot inside the app.
    register_post_type('isokovibe_banner', array(
        'labels' => array(
            'name'          => 'App Banners',
            'singular_name' => 'Banner',
            'add_new'       => 'New Banner',
            'add_new_item'  => 'New Banner',
            'edit_item'     => 'Edit Banner',
            'not_found'     => 'No banners yet.',
        ),
        'public'       => false,
        'show_ui'      => true,
        'show_in_menu' => 'edit.php?post_type=isokovibe_announce',
        'supports'     => array('title', 'thumbnail'),
    ));
}

/* -------------------------------------------------------------------------
 * Announcement fields
 * ---------------------------------------------------------------------- */

add_action('add_meta_boxes', 'isokovibe_app_control_meta_boxes');
function isokovibe_app_control_meta_boxes()
{
    add_meta_box(
        'isokovibe_announce_link',
        'Where should tapping the notification go?',
        'isokovibe_app_control_announce_box',
        'isokovibe_announce',
        'normal',
        'high'
    );
    add_meta_box(
        'isokovibe_banner_fields',
        'Banner settings',
        'isokovibe_app_control_banner_box',
        'isokovibe_banner',
        'normal',
        'high'
    );
}

function isokovibe_app_control_announce_box($post)
{
    wp_nonce_field('isokovibe_app_control_save', 'isokovibe_app_control_nonce');
    $url = get_post_meta($post->ID, '_isokovibe_link_url', true);
    ?>
    <p>
        <label for="isokovibe_link_url"><strong>Link</strong></label><br />
        <input type="url" id="isokovibe_link_url" name="isokovibe_link_url"
               value="<?php echo esc_attr($url); ?>" class="widefat"
               placeholder="https://isokovibe.com.ng/your-song-post" />
        <span class="description">
            Opens in the browser when the notification is tapped. Leave blank to just show the message.
        </span>
    </p>
    <p class="description">
        <strong>The title</strong> above becomes the notification's heading, and
        <strong>the body</strong> becomes its text. Keep both short — Android
        truncates long notifications on the lock screen.
        Publishing sends it; saving a draft does not.
    </p>
    <?php
}

function isokovibe_app_control_banner_box($post)
{
    wp_nonce_field('isokovibe_app_control_save', 'isokovibe_app_control_nonce');
    $url       = get_post_meta($post->ID, '_isokovibe_link_url', true);
    $placement = get_post_meta($post->ID, '_isokovibe_placement', true);
    $sponsored = get_post_meta($post->ID, '_isokovibe_sponsored', true);
    if ($placement === '') {
        $placement = 'footer';
    }
    ?>
    <p>
        <label for="isokovibe_link_url"><strong>Click-through URL</strong></label><br />
        <input type="url" id="isokovibe_link_url" name="isokovibe_link_url"
               value="<?php echo esc_attr($url); ?>" class="widefat"
               placeholder="https://example.com/your-client" />
    </p>
    <p>
        <label for="isokovibe_placement"><strong>Where in the app</strong></label><br />
        <select id="isokovibe_placement" name="isokovibe_placement">
            <option value="footer" <?php selected($placement, 'footer'); ?>>Footer (below the tabs)</option>
            <option value="header" <?php selected($placement, 'header'); ?>>Header (below the app title)</option>
            <option value="both"   <?php selected($placement, 'both'); ?>>Both</option>
        </select>
    </p>
    <p>
        <label>
            <input type="checkbox" name="isokovibe_sponsored" value="1" <?php checked($sponsored, '1'); ?> />
            Mark as "Sponsored"
        </label><br />
        <span class="description">
            Tick this for affiliate and paid placements — most affiliate
            programmes require the disclosure, and it shows as a small label
            on the banner.
        </span>
    </p>
    <p class="description">
        <strong>Set the Featured image</strong> to the banner artwork. Design it
        at <strong>330&times;70</strong> (or any 33:7 ratio — 660&times;140
        looks sharper on high-density screens). The app scales it to fit the
        screen width and keeps the ratio, so anything far off 33:7 will be
        letterboxed.
        Publish to make it live; switch to Draft to pull it immediately.
    </p>
    <?php
}

add_action('save_post', 'isokovibe_app_control_save_meta');
function isokovibe_app_control_save_meta($post_id)
{
    if (!isset($_POST['isokovibe_app_control_nonce'])
        || !wp_verify_nonce($_POST['isokovibe_app_control_nonce'], 'isokovibe_app_control_save')) {
        return;
    }
    if (defined('DOING_AUTOSAVE') && DOING_AUTOSAVE) {
        return;
    }
    if (!current_user_can('edit_post', $post_id)) {
        return;
    }

    if (isset($_POST['isokovibe_link_url'])) {
        update_post_meta($post_id, '_isokovibe_link_url', esc_url_raw($_POST['isokovibe_link_url']));
    }
    if (isset($_POST['isokovibe_placement'])) {
        $placement = in_array($_POST['isokovibe_placement'], array('header', 'footer', 'both'), true)
            ? $_POST['isokovibe_placement']
            : 'footer';
        update_post_meta($post_id, '_isokovibe_placement', $placement);
    }
    update_post_meta($post_id, '_isokovibe_sponsored', isset($_POST['isokovibe_sponsored']) ? '1' : '0');
}

/* -------------------------------------------------------------------------
 * App update settings
 * ---------------------------------------------------------------------- */

add_action('admin_menu', 'isokovibe_app_control_settings_menu');
function isokovibe_app_control_settings_menu()
{
    add_submenu_page(
        'edit.php?post_type=isokovibe_announce',
        'App Update',
        'App Update',
        'manage_options',
        'isokovibe-app-update',
        'isokovibe_app_control_settings_page'
    );
}

add_action('admin_init', 'isokovibe_app_control_register_settings');
function isokovibe_app_control_register_settings()
{
    register_setting('isokovibe_app_update', 'isokovibe_app_version_code', array(
        'type'              => 'integer',
        'sanitize_callback' => 'absint',
        'default'           => 0,
    ));
    register_setting('isokovibe_app_update', 'isokovibe_app_version_name', array(
        'type'              => 'string',
        'sanitize_callback' => 'sanitize_text_field',
        'default'           => '',
    ));
    register_setting('isokovibe_app_update', 'isokovibe_app_download_url', array(
        'type'              => 'string',
        'sanitize_callback' => 'esc_url_raw',
        'default'           => '',
    ));
    register_setting('isokovibe_app_update', 'isokovibe_app_update_message', array(
        'type'              => 'string',
        'sanitize_callback' => 'sanitize_textarea_field',
        'default'           => '',
    ));
    register_setting('isokovibe_app_update', 'isokovibe_app_update_required', array(
        'type'              => 'boolean',
        'sanitize_callback' => 'rest_sanitize_boolean',
        'default'           => false,
    ));
}

function isokovibe_app_control_settings_page()
{
    ?>
    <div class="wrap">
        <h1>App Update</h1>
        <p>
            The app checks these values on launch. When the version code below is
            <strong>higher</strong> than the version installed on a phone, that
            phone shows an update prompt pointing at the download URL.
        </p>
        <form method="post" action="options.php">
            <?php settings_fields('isokovibe_app_update'); ?>
            <table class="form-table" role="presentation">
                <tr>
                    <th scope="row"><label for="isokovibe_app_version_code">Latest version code</label></th>
                    <td>
                        <input type="number" min="0" id="isokovibe_app_version_code"
                               name="isokovibe_app_version_code"
                               value="<?php echo esc_attr(get_option('isokovibe_app_version_code', 0)); ?>" />
                        <p class="description">
                            A whole number that only ever goes up. It must match the
                            version code of the build you published — the app compares
                            against its own, so setting this too high prompts everyone
                            forever, and too low prompts nobody.
                        </p>
                    </td>
                </tr>
                <tr>
                    <th scope="row"><label for="isokovibe_app_version_name">Version name</label></th>
                    <td>
                        <input type="text" id="isokovibe_app_version_name" name="isokovibe_app_version_name"
                               value="<?php echo esc_attr(get_option('isokovibe_app_version_name', '')); ?>"
                               class="regular-text" placeholder="0.1.20" />
                        <p class="description">Shown to the user, e.g. "Version 0.1.20 is available".</p>
                    </td>
                </tr>
                <tr>
                    <th scope="row"><label for="isokovibe_app_download_url">Download URL</label></th>
                    <td>
                        <input type="url" id="isokovibe_app_download_url" name="isokovibe_app_download_url"
                               value="<?php echo esc_attr(get_option('isokovibe_app_download_url', '')); ?>"
                               class="regular-text" placeholder="https://isokovibe.com.ng/app" />
                        <p class="description">Where "Update" sends people. Opens in their browser.</p>
                    </td>
                </tr>
                <tr>
                    <th scope="row"><label for="isokovibe_app_update_message">What's new</label></th>
                    <td>
                        <textarea id="isokovibe_app_update_message" name="isokovibe_app_update_message"
                                  rows="3" class="large-text"><?php
                            echo esc_textarea(get_option('isokovibe_app_update_message', ''));
                        ?></textarea>
                    </td>
                </tr>
                <tr>
                    <th scope="row">Required update</th>
                    <td>
                        <label>
                            <input type="checkbox" name="isokovibe_app_update_required" value="1"
                                <?php checked(get_option('isokovibe_app_update_required'), true); ?> />
                            Users cannot dismiss the prompt
                        </label>
                        <p class="description">
                            Use sparingly. This blocks the app until they update, so
                            anyone who can't download right then is locked out.
                        </p>
                    </td>
                </tr>
            </table>
            <?php submit_button(); ?>
        </form>
    </div>
    <?php
}

/* -------------------------------------------------------------------------
 * REST endpoint
 * ---------------------------------------------------------------------- */

add_action('rest_api_init', 'isokovibe_app_control_register_routes');
function isokovibe_app_control_register_routes()
{
    register_rest_route('isokovibe/v1', '/app-config', array(
        'methods'             => 'GET',
        'callback'            => 'isokovibe_app_control_config',
        'permission_callback' => '__return_true',
    ));
}

function isokovibe_app_control_config()
{
    return rest_ensure_response(array(
        'announcement' => isokovibe_app_control_latest_announcement(),
        'app_version'  => isokovibe_app_control_version_payload(),
        'banners'      => isokovibe_app_control_banners(),
    ));
}

/**
 * Only the most recent announcement is served. The app tracks the last id it
 * showed, so sending a second one supersedes the first rather than queueing
 * a backlog that all fires at once on a phone that was offline for a week.
 */
function isokovibe_app_control_latest_announcement()
{
    $posts = get_posts(array(
        'post_type'        => 'isokovibe_announce',
        'post_status'      => 'publish',
        'numberposts'      => 1,
        'orderby'          => 'date',
        'order'            => 'DESC',
        'suppress_filters' => false,
    ));
    if (empty($posts)) {
        return null;
    }

    $post = $posts[0];
    return array(
        'id'           => (int) $post->ID,
        'title'        => wp_strip_all_tags(get_the_title($post)),
        'body'         => wp_strip_all_tags(strip_shortcodes($post->post_content)),
        'url'          => (string) get_post_meta($post->ID, '_isokovibe_link_url', true),
        'published_at' => (int) get_post_time('U', true, $post),
    );
}

function isokovibe_app_control_version_payload()
{
    $code = (int) get_option('isokovibe_app_version_code', 0);
    if ($code <= 0) {
        return null;
    }
    return array(
        'version_code' => $code,
        'version_name' => (string) get_option('isokovibe_app_version_name', ''),
        'download_url' => (string) get_option('isokovibe_app_download_url', ''),
        'message'      => (string) get_option('isokovibe_app_update_message', ''),
        'required'     => (bool) get_option('isokovibe_app_update_required', false),
    );
}

function isokovibe_app_control_banners()
{
    $posts = get_posts(array(
        'post_type'        => 'isokovibe_banner',
        'post_status'      => 'publish',
        'numberposts'      => 20,
        'orderby'          => 'menu_order date',
        'order'            => 'ASC',
        'suppress_filters' => false,
    ));

    $banners = array();
    foreach ($posts as $post) {
        $image = get_the_post_thumbnail_url($post->ID, 'full');
        // A banner with no artwork has nothing to show, so it's skipped
        // rather than sent as an empty slot the app has to reason about.
        if (!$image) {
            continue;
        }
        $banners[] = array(
            'id'        => (int) $post->ID,
            'image_url' => $image,
            'link_url'  => (string) get_post_meta($post->ID, '_isokovibe_link_url', true),
            'placement' => (string) (get_post_meta($post->ID, '_isokovibe_placement', true) ?: 'footer'),
            'sponsored' => get_post_meta($post->ID, '_isokovibe_sponsored', true) === '1',
        );
    }
    return $banners;
}
