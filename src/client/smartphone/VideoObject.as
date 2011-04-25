/**
 * Flash Video object wrapper
 * Flex can't display Video object in stage
 * Need to wrap it with flex UIComponent
 */
package
{
	import flash.media.Video;
	import mx.core.UIComponent;
	
	public class VideoObject extends UIComponent
	{
		public var _video:flash.media.Video;
		
		public function VideoObject() {
				super();
				
				_video = new flash.media.Video(); 
	            this.addChild(_video);
		}

		public function get video():Video {
			return _video;
		}
		
		public function set video(v:Video):void {
			_video = v;
		}
		
	}
	

}