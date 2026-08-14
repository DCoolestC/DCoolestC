<?php
/**
 * Plugin Name: iSokoVibe Custom Ads
 * Description: Manage local/affiliate ad banners shown in the iSokoVibe Android app, right from WordPress.
 * Version: 1.0.0
 * Author: iSokoVibe
 * License: GPL-2.0-or-later
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit; // No direct access.
}

// ---------------------------------------------------------------------
// "App Ads" post type — one post per ad banner. Publish = live,
// Draft/Trash = off. No separate "active" checkbox needed; that's what
// post status already is.
// ---------------------------------------------------------------------

add_action( 'init', function () {
	register_post_type( 'isokovibe_ad', array(
		'label'        => 'App Ads',
		'labels'       => array(
			'name'          => 'App Ads',
			'singular_name' => 'App Ad',
			'add_new_item'  => 'Add New Ad',
			'edit_item'     => 'Edit Ad',
			'all_items'     => 'All Ads',
		),
		'public'       => false,
		'show_ui'      => true,
		'show_in_menu' => true,
		'menu_icon'    => 'dashicons-megaphone',
		'supports'     => array( 'title', 'thumbnail' ),
		'show_in_rest' => false, // We expose our own curated endpoint below instead.
	) );
} );

// ---------------------------------------------------------------------
// Meta box: click-through URL, sponsored label, priority.
// ---------------------------------------------------------------------

add_action( 'add_meta_boxes', function () {
	add_meta_box(
		'isokovibe_ad_details',
		'Ad Details',
		'isokovibe_ad_render_meta_box',
		'isokovibe_ad',
		'normal',
		'high'
	);
} );

function isokovibe_ad_render_meta_box( $post ) {
	wp_nonce_field( 'isokovibe_ad_save', 'isokovibe_ad_nonce' );

	$click_url = get_post_meta( $post->ID, '_isokovibe_click_url', true );
	$sponsored = get_post_meta( $post->ID, '_isokovibe_sponsored', true );
	$priority  = get_post_meta( $post->ID, '_isokovibe_priority', true );
	if ( '' === $priority ) {
		$priority = 10;
	}
	?>
	<p>
		<label for="isokovibe_click_url"><strong>Click-through URL</strong></label><br>
		<input type="url" id="isokovibe_click_url" name="isokovibe_click_url"
			value="<?php echo esc_attr( $click_url ); ?>" class="widefat" placeholder="https://…" required />
		<span class="description">Where the app opens a browser to when someone taps this ad — your own page, an affiliate link, whatever.</span>
	</p>
	<p>
		<label>
			<input type="checkbox" name="isokovibe_sponsored" <?php checked( $sponsored, '1' ); ?> />
			Show a "Sponsored" label on the ad
		</label>
		<br><span class="description">Turn this on for affiliate links — standard practice to disclose sponsored content.</span>
	</p>
	<p>
		<label for="isokovibe_priority"><strong>Priority</strong></label><br>
		<input type="number" id="isokovibe_priority" name="isokovibe_priority" value="<?php echo esc_attr( $priority ); ?>" style="width:80px" />
		<span class="description">Lower numbers show first when there are several active ads. Default 10.</span>
	</p>
	<p class="description">
		Set the <strong>Featured Image</strong> (in the sidebar) to the ad banner — a wide image works best,
		roughly 1200×200px. <strong>Publish</strong> this post to make the ad live in the app; move it to
		Draft or Trash to stop showing it.
	</p>
	<?php
}

add_action( 'save_post_isokovibe_ad', function ( $post_id ) {
	if ( ! isset( $_POST['isokovibe_ad_nonce'] ) || ! wp_verify_nonce( $_POST['isokovibe_ad_nonce'], 'isokovibe_ad_save' ) ) {
		return;
	}
	if ( defined( 'DOING_AUTOSAVE' ) && DOING_AUTOSAVE ) {
		return;
	}
	if ( ! current_user_can( 'edit_post', $post_id ) ) {
		return;
	}

	if ( isset( $_POST['isokovibe_click_url'] ) ) {
		update_post_meta( $post_id, '_isokovibe_click_url', esc_url_raw( wp_unslash( $_POST['isokovibe_click_url'] ) ) );
	}
	update_post_meta( $post_id, '_isokovibe_sponsored', isset( $_POST['isokovibe_sponsored'] ) ? '1' : '' );
	if ( isset( $_POST['isokovibe_priority'] ) ) {
		update_post_meta( $post_id, '_isokovibe_priority', intval( $_POST['isokovibe_priority'] ) );
	}
} );

// Nudge: list view shows which ads are missing an image or URL, so
// nothing gets published half-configured by accident.
add_filter( 'manage_isokovibe_ad_posts_columns', function ( $columns ) {
	$columns['isokovibe_ready'] = 'Ready?';
	return $columns;
} );

add_action( 'manage_isokovibe_ad_posts_custom_column', function ( $column, $post_id ) {
	if ( 'isokovibe_ready' !== $column ) {
		return;
	}
	$has_image = has_post_thumbnail( $post_id );
	$has_url   = (bool) get_post_meta( $post_id, '_isokovibe_click_url', true );
	echo ( $has_image && $has_url ) ? '✅' : '⚠️ missing image or URL';
}, 10, 2 );

// ---------------------------------------------------------------------
// REST endpoint the Android app polls: GET /wp-json/isokovibe/v1/ads
// ---------------------------------------------------------------------

add_action( 'rest_api_init', function () {
	register_rest_route( 'isokovibe/v1', '/ads', array(
		'methods'             => 'GET',
		'callback'            => 'isokovibe_ads_rest_response',
		'permission_callback' => '__return_true',
	) );
} );

function isokovibe_ads_rest_response() {
	$query = new WP_Query( array(
		'post_type'      => 'isokovibe_ad',
		'post_status'    => 'publish',
		'posts_per_page' => 20,
		'meta_key'       => '_isokovibe_priority',
		'orderby'        => 'meta_value_num',
		'order'          => 'ASC',
	) );

	$ads = array();
	foreach ( $query->posts as $post ) {
		$image     = get_the_post_thumbnail_url( $post->ID, 'large' );
		$click_url = get_post_meta( $post->ID, '_isokovibe_click_url', true );
		if ( ! $image || ! $click_url ) {
			continue; // Both are required for the ad to be usable.
		}

		$ads[] = array(
			'id'         => $post->ID,
			'title'      => get_the_title( $post ),
			'image_url'  => $image,
			'click_url'  => $click_url,
			'sponsored'  => (bool) get_post_meta( $post->ID, '_isokovibe_sponsored', true ),
			'priority'   => (int) get_post_meta( $post->ID, '_isokovibe_priority', true ),
		);
	}

	$response = new WP_REST_Response( $ads, 200 );
	$response->header( 'Cache-Control', 'public, max-age=300' );
	return $response;
}
